package com.davidhobbet.audiobooks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
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

/** Tap a chapter to jump to it; returns the chapter index to the player. */
class ChapterListScreen(
    sealedActivity: SealedLightActivity,
    private val book: Book,
    private val currentIndex: Int,
) : SimpleLightScreen<Int>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()

        LightTheme(colors = themeColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LightTopBar(
                        center = LightTopBarCenter.Text("CHAPTERS"),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )
                    LightScrollView(modifier = Modifier.fillMaxSize()) {
                        book.chapters.forEach { chapter ->
                            val isCurrent = chapter.index == currentIndex
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .lightClickable { goBack(chapter.index) }
                                    .padding(
                                        horizontal = 1f.gridUnitsAsDp(),
                                        vertical = 0.6f.gridUnitsAsDp(),
                                    ),
                            ) {
                                if (isCurrent) {
                                    LightIcon(
                                        icon = LightIcons.PLAY,
                                        size = 1.5f,
                                        modifier = Modifier.padding(end = 0.5f.gridUnitsAsDp()),
                                    )
                                }
                                LightText(
                                    text = "${chapter.index + 1}. ${chapter.title.uppercase()}",
                                    variant = LightTextVariant.Copy,
                                    lighten = !isCurrent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
