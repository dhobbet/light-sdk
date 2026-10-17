package com.davidhobbet.audiobooks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.audio.DefaultLightAudio
import com.thelightphone.sdk.audio.LightAudio
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIconConfiguration
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable

class PlayerViewModel(
    private val book: Book,
    private val progressStore: ProgressStore,
    private val audio: LightAudio,
) : LightViewModel<Unit>() {

    val playerState = PlayerController.state

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        PlayerController.attach(progressStore)
        PlayerController.bind(audio)
        PlayerController.openBook(book)
    }

    override fun onAppPause() {
        PlayerController.onAppPaused()
    }
}

class PlayerScreen(
    private val sealedActivity: SealedLightActivity,
    private val book: Book,
) : LightScreen<Unit, PlayerViewModel>(sealedActivity) {

    override val viewModelClass: Class<PlayerViewModel>
        get() = PlayerViewModel::class.java

    override fun createViewModel(): PlayerViewModel = PlayerViewModel(
        book = book,
        progressStore = ProgressStore(lightContext.dataStore),
        audio = DefaultLightAudio(sealedActivity),
    )

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val state by viewModel.playerState.collectAsState()

        val showsThisBook = state.book?.id == book.id
        val chapterIndex = if (showsThisBook) state.chapterIndex else 0
        val chapter = book.chapters.getOrNull(chapterIndex)

        LightTheme(colors = themeColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LightTopBar(
                        center = LightTopBarCenter.Text(book.title.uppercase()),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    LightText(
                        text = chapter?.title?.uppercase() ?: "NO CHAPTER",
                        variant = LightTextVariant.Subheading,
                        align = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 1.5f.gridUnitsAsDp()),
                    )
                    Spacer(modifier = Modifier.height(0.5f.gridUnitsAsDp()))
                    LightText(
                        text = "CHAPTER ${chapterIndex + 1} OF ${book.chapters.size}",
                        variant = LightTextVariant.Detail,
                        lighten = true,
                    )
                    Spacer(modifier = Modifier.height(0.5f.gridUnitsAsDp()))
                    LightText(
                        text = if (showsThisBook) {
                            "${formatTime(state.positionMs)} / ${formatTime(state.durationMs)}"
                        } else {
                            "LOADING..."
                        },
                        variant = LightTextVariant.Detail,
                        monospace = true,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 1f.gridUnitsAsDp()),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ControlButton(LightIcons.REWIND, "CHAPTER") {
                            PlayerController.previousChapter()
                        }
                        ControlButton(LightIcons.SKIP_BACKWARD_FIFTEEN, "15 SEC") {
                            PlayerController.skipBack()
                        }
                        ControlButton(
                            icon = if (showsThisBook && state.isPlaying) LightIcons.PAUSE else LightIcons.PLAY,
                            label = if (showsThisBook && state.isPlaying) "PAUSE" else "PLAY",
                            size = 4f,
                        ) {
                            PlayerController.togglePlayPause()
                        }
                        ControlButton(LightIcons.SKIP_FORWARD_FIFTEEN, "15 SEC") {
                            PlayerController.skipForward()
                        }
                        ControlButton(LightIcons.FAST_FORWARD, "CHAPTER") {
                            PlayerController.nextChapter()
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    SettingRow("SPEED · ${speedLabel(state.speed)}") {
                        PlayerController.cycleSpeed()
                    }
                    SettingRow("SLEEP · ${sleepLabel(state.sleepRemainingMs)}") {
                        PlayerController.cycleSleepTimer()
                    }
                    SettingRow("CHAPTER LIST") {
                        navigateTo(
                            screenFactory = { ChapterListScreen(it, book, state.chapterIndex) },
                            resultCallback = { index -> PlayerController.jumpToChapter(index) },
                        )
                    }

                    Spacer(modifier = Modifier.height(1f.gridUnitsAsDp()))
                }
            }
        }
    }

    @Composable
    private fun ControlButton(
        icon: LightIconConfiguration,
        label: String,
        size: Float = 2.5f,
        onClick: () -> Unit,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .lightClickable(onClick = onClick)
                .padding(0.5f.gridUnitsAsDp()),
        ) {
            LightIcon(icon = icon, size = size)
            LightText(text = label, variant = LightTextVariant.Micro, lighten = true)
        }
    }

    @Composable
    private fun SettingRow(text: String, onClick: () -> Unit) {
        LightText(
            text = text,
            variant = LightTextVariant.Copy,
            align = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .lightClickable(onClick = onClick)
                .padding(vertical = 0.6f.gridUnitsAsDp()),
        )
    }

    private fun speedLabel(speed: Float): String {
        val trimmed = speed.toString().trimEnd('0').trimEnd('.')
        return "${trimmed}X"
    }

    private fun sleepLabel(remainingMs: Long?): String =
        if (remainingMs == null) "OFF" else formatTime(remainingMs)
}
