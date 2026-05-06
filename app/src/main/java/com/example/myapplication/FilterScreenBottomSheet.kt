package com.example.myapplication

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapHorizontalCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@Composable
fun FilterScreenBottomSheet(context: Context, imgState: FilterScreenImgState) {
    var showPlainText by remember { mutableStateOf(false) }
    val textToDisplay = if (showPlainText) {
        imgState.rawOcrText
    } else {
        imgState.formattedOcrText
    }
    val clipboard = LocalClipboard.current
    val clipEntryString = ClipEntry(clipData = ClipData.newPlainText("Recognised Text", textToDisplay))
    var fontSize by remember { mutableFloatStateOf(9f) }

    // INIT AND HANDLE TTS
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var isPlaying by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    Row {
        // PLAY BUTTON
        IconButton(
            onClick = {
                if (isPlaying) {
                    tts?.stop()
                    isPlaying = false
                } else {
                    tts?.speak(textToDisplay, TextToSpeech.QUEUE_FLUSH, null, "tts1")
                    isPlaying = true
                }
            }
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause Text To Speech" else "Play Text To Speech",
            )
        }

        // CLIPBOARD BUTTON
        val clipboardCoroutine = rememberCoroutineScope() // outside onClick, can only be called in a Composable
        IconButton(
            onClick = {
                clipboardCoroutine.launch {
                    clipboard.setClipEntry(clipEntryString)}
            }
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy to clipboard",
            )
        }

        // DOWNLOAD
        IconButton(
            onClick = {saveToGallery(context, null, imgState.filtered)}
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download Image",
            )
        }

        // SHARE
        var isExpanded by remember {mutableStateOf(false)}
        IconButton(onClick = {isExpanded = !isExpanded}) {      // toggle isExpanded
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share image or text"
            )
            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = {isExpanded = false}
            ) {
                DropdownMenuItem(
                    text = {Text("Share Image")},
                    onClick = {
                        val photoFile = File(                   // create temp file (cache) t0 hold current img
                            context.cacheDir,
                            "photo_${System.currentTimeMillis()}.jpeg"
                        )
                        FileOutputStream(photoFile).use { outputStream ->
                            imgState.filtered!!.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)   // write img to file
                        }
                        val photoUri = FileProvider.getUriForFile(
                            context,
                            context.packageName + ".provider",
                            photoFile
                        )
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_STREAM, photoUri)
                            type = "image/jpeg"
                        }
                        startActivity(context, Intent.createChooser(sendIntent, null), null)
                    }
                )
                DropdownMenuItem(
                    text = {Text("Share Text")},
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, textToDisplay)
                            type = "text/plain"
                        }
                        startActivity(context, Intent.createChooser(sendIntent, null), null)
                    }
                )
            }
        }

        // ADJUST FONT SIZE
        Slider(
            value = fontSize,
            onValueChange = { fontSize = it },
            modifier = Modifier.weight(1f).padding(8.dp).height(24.dp),
            valueRange = 2f..16f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.tertiary,
                activeTickColor = MaterialTheme.colorScheme.tertiary,
                inactiveTickColor = MaterialTheme.colorScheme.tertiary,

                activeTrackColor = MaterialTheme.colorScheme.tertiaryContainer,
                inactiveTrackColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
            steps = 7
        )

        //TOGGLE TEXT FORMAT
        IconButton(
            onClick = {
                showPlainText = !showPlainText
            }
        ) {
            Icon(
                imageVector = if (showPlainText) Icons.Default.SwapHorizontalCircle else Icons.Default.SwapHoriz,
                contentDescription = "Toggle Text Format",
            )
        }
    }
    HorizontalDivider()

    // DISPLAYS RECOGNISED TEXT
    SelectionContainer(modifier = Modifier.padding(8.dp)) { // wrapped to allow selection gestures
        LazyColumn { item  { Text(text = textToDisplay, fontFamily = FontFamily.Monospace, fontSize = fontSize.sp) }}
    }
}