package com.davidhobbet.audiobooks

import com.thelightphone.sdk.audio.LightAudio
import com.thelightphone.sdk.audio.LightAudioItem
import com.thelightphone.sdk.audio.LightAudioPlayback
import com.thelightphone.sdk.audio.LightAudioPlayer
import com.thelightphone.sdk.audio.LightAudioSource
import com.thelightphone.sdk.audio.LightAudioUsage
import com.thelightphone.sdk.audio.LightMediaMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerState(
    val book: Book? = null,
    val chapterIndex: Int = 0,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1.0f,
    val sleepMinutes: Int? = null,
    val sleepRemainingMs: Long? = null,
) {
    val chapter: Chapter? get() = book?.chapters?.getOrNull(chapterIndex)
}

/**
 * One detached LightAudioPlayer for the whole tool. Detached playback is owned
 * by the SDK's media service, so audio continues when the user leaves the tool
 * or the screen sleeps, and the system/bluetooth media controls work. A book is
 * one queue; a chapter is one queue item, so auto-advance is the player's job.
 */
object PlayerController {

    val SPEED_OPTIONS = listOf(1.0f, 1.25f, 1.5f, 2.0f)
    val SLEEP_OPTIONS = listOf<Int?>(null, 15, 30, 60)

    private const val SAVE_INTERVAL_MS = 5_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: LightAudioPlayer? = null
    private var progressStore: ProgressStore? = null
    private var sleepJob: Job? = null

    private val currentBook = MutableStateFlow<Book?>(null)
    private val speedFlow = MutableStateFlow(1.0f)
    private val sleepFlow = MutableStateFlow<Pair<Int, Long>?>(null) // minutes, remaining ms

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /** Called once so resume points and speed survive across sessions. */
    fun attach(store: ProgressStore) {
        if (progressStore != null) return
        progressStore = store
        scope.launch {
            val speed = store.loadSpeed()
            speedFlow.value = speed
            player?.speed = speed
        }
    }

    /**
     * Create the tool's one detached player, or reconnect to a live detached
     * session. Safe to call from every screen; only the first call does work.
     * The handle is kept for the life of the process: releasing it would not
     * stop detached playback, and every screen shares it.
     */
    fun bind(audio: LightAudio) {
        if (player != null) return
        val p = audio.newPlayer(
            usage = LightAudioUsage.Speech,
            playback = LightAudioPlayback.Detached,
        )
        p.speed = speedFlow.value
        player = p
        mirrorState(p)
        runProgressSaver(p)
    }

    /**
     * Show [book] in the player: reconnect to it if its queue is already the
     * live detached session, otherwise load it at the saved resume point, paused.
     */
    fun openBook(book: Book) {
        val p = player ?: return
        scope.launch {
            if (!p.awaitReady()) return@launch
            val store = progressStore
            val liveBookId = store?.loadLastBook()
            if (liveBookId == book.id && p.currentMediaItemIndex.value >= 0) {
                // The detached session is already playing this book.
                currentBook.value = book
                return@launch
            }
            saveNow()
            val saved = store?.loadProgress(book.id)
            val start = (saved?.chapterIndex ?: 0).coerceIn(0, book.chapters.lastIndex)
            currentBook.value = book
            store?.saveLastBook(book.id)
            p.setMediaQueue(book.chapters.map(::queueItem), startIndex = start)
            val position = saved?.positionMs ?: 0L
            if (position > 0L) p.seekTo(position)
        }
    }

    /**
     * After a tool relaunch with a detached session still live, recover which
     * book the session belongs to (the last book this tool opened).
     */
    fun recoverBook(books: List<Book>) {
        if (currentBook.value != null) return
        val p = player ?: return
        scope.launch {
            if (!p.awaitReady()) return@launch
            if (p.currentMediaItemIndex.value < 0) return@launch
            val liveBookId = progressStore?.loadLastBook() ?: return@launch
            currentBook.value = books.firstOrNull { it.id == liveBookId }
        }
    }

    fun togglePlayPause() {
        val p = player ?: return
        if (p.isPlaying.value) {
            p.pause()
            scope.launch { saveNow() }
        } else {
            p.play()
        }
    }

    fun nextChapter() { player?.skipToNext() }

    /** Restarts the current chapter; pressed again near its start, goes back one. */
    fun previousChapter() { player?.skipToPrevious() }

    /** Seeks backward 15 seconds within the chapter. */
    fun skipBack() { player?.skipBack() }

    /** Seeks forward 15 seconds within the chapter. */
    fun skipForward() { player?.skipForward() }

    /** Jumps to a chapter. Adjacent jumps keep the queue; others reload it. */
    fun jumpToChapter(index: Int) {
        val p = player ?: return
        val book = currentBook.value ?: return
        val target = index.coerceIn(0, book.chapters.lastIndex)
        val current = p.currentMediaItemIndex.value
        when (target) {
            current -> return
            current + 1 -> p.skipToNext()
            current - 1 -> p.skipToPrevious()
            else -> {
                val wasPlaying = p.isPlaying.value
                p.setMediaQueue(book.chapters.map(::queueItem), startIndex = target)
                if (wasPlaying) p.play()
            }
        }
    }

    fun cycleSpeed() {
        val next = SPEED_OPTIONS[(SPEED_OPTIONS.indexOf(speedFlow.value) + 1).mod(SPEED_OPTIONS.size)]
        speedFlow.value = next
        player?.speed = next
        scope.launch { progressStore?.saveSpeed(next) }
    }

    fun cycleSleepTimer() {
        val next = SLEEP_OPTIONS[(SLEEP_OPTIONS.indexOf(sleepFlow.value?.first) + 1).mod(SLEEP_OPTIONS.size)]
        setSleepTimer(next)
    }

    fun setSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        sleepJob = null
        if (minutes == null) {
            sleepFlow.value = null
            return
        }
        sleepFlow.value = minutes to minutes * 60_000L
        sleepJob = scope.launch {
            var remaining = minutes * 60_000L
            while (remaining > 0L && isActive) {
                delay(1_000L)
                remaining -= 1_000L
                sleepFlow.value = minutes to remaining.coerceAtLeast(0L)
            }
            if (isActive) {
                player?.pause()
                saveNow()
                sleepFlow.value = null
            }
        }
    }

    /** Persist the resume point whenever the tool goes to the background. */
    fun onAppPaused() {
        scope.launch { saveNow() }
    }

    private fun mirrorState(p: LightAudioPlayer) {
        scope.launch {
            combine(
                p.isPlaying, p.positionMs, p.durationMs, p.currentMediaItemIndex,
                combine(currentBook, speedFlow, sleepFlow) { book, speed, sleep ->
                    Triple(book, speed, sleep)
                },
            ) { playing, position, duration, index, extras ->
                val (book, speed, sleep) = extras
                PlayerState(
                    book = book,
                    chapterIndex = index.coerceAtLeast(0),
                    isPlaying = playing,
                    positionMs = position,
                    durationMs = duration,
                    speed = speed,
                    sleepMinutes = sleep?.first,
                    sleepRemainingMs = sleep?.second,
                )
            }.collect { _state.value = it }
        }
    }

    private fun runProgressSaver(p: LightAudioPlayer) {
        scope.launch {
            while (isActive) {
                delay(SAVE_INTERVAL_MS)
                if (p.isPlaying.value) saveNow()
            }
        }
    }

    private suspend fun saveNow() {
        val p = player ?: return
        val book = currentBook.value ?: return
        val index = p.currentMediaItemIndex.value
        if (index < 0) return
        progressStore?.saveProgress(book.id, BookProgress(index, p.positionMs.value))
    }

    private fun queueItem(chapter: Chapter): LightAudioItem = LightAudioItem(
        source = LightAudioSource.FileSource(chapter.file),
        metadata = LightMediaMetadata(
            title = chapter.title,
            album = currentBook.value?.title,
            artist = currentBook.value?.title,
        ),
    )
}
