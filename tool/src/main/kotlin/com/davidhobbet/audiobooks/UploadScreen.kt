package com.davidhobbet.audiobooks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UploadViewModel(
    private val library: AudiobookLibrary,
) : LightViewModel<Unit>() {

    private val server = UploadServer(library) { refresh() }

    val books = MutableStateFlow<List<Book>>(emptyList())
    val addresses = MutableStateFlow<List<String>>(emptyList())

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        viewModelScope.launch(Dispatchers.IO) {
            server.start()
            addresses.value = server.addresses()
        }
        refresh()
    }

    override fun onScreenHide(screen: SimpleLightScreen<Unit>) {
        stopServer()
    }

    override fun onAppPause() {
        stopServer()
    }

    private fun stopServer() {
        viewModelScope.launch(Dispatchers.IO) { server.stop() }
    }

    private fun refresh() {
        viewModelScope.launch {
            books.value = withContext(Dispatchers.IO) { library.scan() }
        }
    }
}

class UploadScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, UploadViewModel>(sealedActivity) {

    override val viewModelClass: Class<UploadViewModel>
        get() = UploadViewModel::class.java

    override fun createViewModel(): UploadViewModel =
        UploadViewModel(AudiobookLibrary(lightContext.filesDir))

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val books by viewModel.books.collectAsState()
        val addresses by viewModel.addresses.collectAsState()

        LightTheme(colors = themeColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LightTopBar(
                        center = LightTopBarCenter.Text("LOAD BOOKS"),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )
                    LightScrollView(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 1f.gridUnitsAsDp()),
                        ) {
                            if (addresses.isEmpty()) {
                                LightText(
                                    text = "No wi-fi address found. Connect this phone to wi-fi, then reopen this screen.",
                                    variant = LightTextVariant.Paragraph,
                                )
                            } else {
                                LightText(
                                    text = "1. Put this phone and your computer on the same wi-fi network.",
                                    variant = LightTextVariant.Paragraph,
                                )
                                Spacer(modifier = Modifier.height(0.75f.gridUnitsAsDp()))
                                LightText(
                                    text = "2. On the computer, open:",
                                    variant = LightTextVariant.Paragraph,
                                )
                                addresses.forEach { address ->
                                    LightText(
                                        text = address,
                                        variant = LightTextVariant.Copy,
                                        monospace = true,
                                        modifier = Modifier.padding(vertical = 0.25f.gridUnitsAsDp()),
                                    )
                                }
                                Spacer(modifier = Modifier.height(0.75f.gridUnitsAsDp()))
                                LightText(
                                    text = "3. Upload each book: enter its title and pick the chapter MP3s. Chapters play in filename order.",
                                    variant = LightTextVariant.Paragraph,
                                )
                                Spacer(modifier = Modifier.height(0.75f.gridUnitsAsDp()))
                                LightText(
                                    text = "Keep this screen open while uploading.",
                                    variant = LightTextVariant.Detail,
                                    lighten = true,
                                )
                            }

                            Spacer(modifier = Modifier.height(1.5f.gridUnitsAsDp()))
                            LightText(
                                text = "ON THE PHONE",
                                variant = LightTextVariant.Subheading,
                            )
                            Spacer(modifier = Modifier.height(0.5f.gridUnitsAsDp()))
                            if (books.isEmpty()) {
                                LightText(
                                    text = "NO BOOKS YET",
                                    variant = LightTextVariant.Detail,
                                    lighten = true,
                                )
                            } else {
                                books.forEach { book ->
                                    LightText(
                                        text = "${book.title.uppercase()} · ${book.chapters.size} CH",
                                        variant = LightTextVariant.Detail,
                                        modifier = Modifier.padding(vertical = 0.35f.gridUnitsAsDp()),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
