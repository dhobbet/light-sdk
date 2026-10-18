package com.davidhobbet.audiobooks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.audio.DefaultLightAudio
import com.thelightphone.sdk.audio.LightAudio
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeScreenViewModel(
    private val library: AudiobookLibrary,
    private val progressStore: ProgressStore,
    private val audio: LightAudio,
) : LightViewModel<Unit>() {

    val books = MutableStateFlow<List<Book>>(emptyList())
    val progress = MutableStateFlow<Map<String, BookProgress>>(emptyMap())
    val inboxChapters = MutableStateFlow(0)

    init {
        PlayerController.attach(progressStore)
        PlayerController.bind(audio)
    }

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        refresh()
    }

    override fun onAppPause() {
        PlayerController.onAppPaused()
    }

    fun fileInboxAs(title: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { library.fileInboxAs(title) }
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val scanned = withContext(Dispatchers.IO) { library.scan() }
            inboxChapters.value = withContext(Dispatchers.IO) { library.pendingInboxChapters() }
            books.value = scanned
            progress.value = progressStore.loadAllProgress()
            PlayerController.recoverBook(scanned)
        }
    }
}

@InitialScreen
class HomeScreen(private val sealedActivity: SealedLightActivity) :
    LightScreen<Unit, HomeScreenViewModel>(sealedActivity) {

    override val viewModelClass: Class<HomeScreenViewModel>
        get() = HomeScreenViewModel::class.java

    override fun createViewModel(): HomeScreenViewModel = HomeScreenViewModel(
        library = AudiobookLibrary(lightContext.filesDir),
        progressStore = ProgressStore(lightContext.dataStore),
        audio = DefaultLightAudio(sealedActivity),
    )

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val books by viewModel.books.collectAsState()
        val progress by viewModel.progress.collectAsState()
        val playerState by PlayerController.state.collectAsState()

        LightTheme(colors = themeColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LightTopBar(
                        center = LightTopBarCenter.Text("AUDIOBOOKS"),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    val inboxCount by viewModel.inboxChapters.collectAsState()
                    if (inboxCount > 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .lightClickable {
                                    navigateTo(::BookTitleEditorScreen) { title ->
                                        if (!title.isNullOrBlank()) viewModel.fileInboxAs(title)
                                    }
                                }
                                .padding(horizontal = 1f.gridUnitsAsDp(), vertical = 0.75f.gridUnitsAsDp()),
                        ) {
                            LightText(
                                text = "NEW BOOK UPLOADED",
                                variant = LightTextVariant.Copy,
                            )
                            LightText(
                                text = "$inboxCount CHAPTERS · TAP TO NAME THE BOOK",
                                variant = LightTextVariant.Detail,
                                lighten = true,
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        if (books.isEmpty()) {
                            LightText(
                                text = "No books yet.\n\nTap LOAD BOOKS OVER WI-FI below to add your first audiobook from a computer.",
                                variant = LightTextVariant.Paragraph,
                                modifier = Modifier.padding(horizontal = 1f.gridUnitsAsDp()),
                            )
                        } else {
                            LightScrollView(modifier = Modifier.fillMaxSize()) {
                                books.forEach { book ->
                                    BookRow(
                                        book = book,
                                        progress = progress[book.id],
                                        isCurrent = playerState.book?.id == book.id,
                                        isPlaying = playerState.isPlaying,
                                    )
                                }
                            }
                        }
                    }

                    LightText(
                        text = "+ LOAD BOOKS OVER WI-FI",
                        variant = LightTextVariant.Copy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .lightClickable { navigateTo(::UploadScreen) }
                            .padding(horizontal = 1f.gridUnitsAsDp(), vertical = 1f.gridUnitsAsDp()),
                    )
                }
            }
        }
    }

    @Composable
    private fun BookRow(book: Book, progress: BookProgress?, isCurrent: Boolean, isPlaying: Boolean) {
        val detail = buildString {
            if (isCurrent && isPlaying) append("PLAYING · ")
            if (progress != null && (progress.chapterIndex > 0 || progress.positionMs > 0L)) {
                append("CH ${progress.chapterIndex + 1} OF ${book.chapters.size}")
            } else {
                append("${book.chapters.size} CHAPTERS")
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .lightClickable { navigateTo({ PlayerScreen(it, book) }) }
                .padding(horizontal = 1f.gridUnitsAsDp(), vertical = 0.75f.gridUnitsAsDp()),
        ) {
            LightText(
                text = book.title.uppercase(),
                variant = LightTextVariant.Copy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LightText(
                text = detail,
                variant = LightTextVariant.Detail,
                lighten = true,
            )
        }
    }
}
