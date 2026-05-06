package com.example.myapplication

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import androidx.core.graphics.createBitmap


@Composable
fun FiltersBar(imgState: FilterScreenImgState, corners: Corners, changeCorners: (Corners) -> Unit, changeImgState: (FilterScreenImgState) -> Unit) {
    val selectedFilter = imgState.selectedFilter
    val contrast = imgState.contrast
    val brightness = imgState.brightness
    val saturation = imgState.saturation
    val sharpen = imgState.sharpen

    val latestCorners by rememberUpdatedState(corners)

    LaunchedEffect(
        imgState.rotated,

        imgState.selectedFilter,
        imgState.contrast,
        imgState.brightness,
        imgState.saturation,
        imgState.sharpen
    ) {
        imgState.rotated?.let { bitmap ->
            val result = withContext(Dispatchers.Default) {
                if (selectedFilter == 0) {
                    // reset
                    bitmap
                } else {
                    applyOpenCVFilters(
                        bitmap,
                        contrast = contrast,
                        brightness = brightness,
                        saturation = saturation,
                        sharpen = sharpen
                    )
                }
            }
            changeImgState(imgState.copy(filtered = result))
        }
    }

    Column {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            state = rememberLazyListState(1,0),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            val filters = listOf(
                "Reset",
                "Contrast",
                "Brightness",
                "Saturation",
                "Sharpness"
            )
            itemsIndexed(filters) { index, label ->
                Button(
                    onClick = {
                        if (index == 0) {
                            changeImgState( imgState.copy(
                                selectedFilter = 0,
                                contrast = 1f,
                                brightness = 0f,
                                saturation = 1f,
                                sharpen = 0f,
                                filtered = imgState.rotated,
                                rawOcrText = imgState.firstRawOcrText,
                                formattedOcrText = imgState.firstFormattedOcrText
                            ))
                            changeCorners(
                                Corners(
                                    topLeft = Offset(400f, 400f),
                                    topRight = Offset(800f, 400f),
                                    bottomRight = Offset(800f, 800f),
                                    bottomLeft = Offset(400f, 800f)
                                )
                            )
                        } else {
                            changeImgState(imgState.copy(selectedFilter = index))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedFilter == index) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.onPrimaryContainer,

                        contentColor = if (selectedFilter == index) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text(label)
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            when (selectedFilter) {                          // sliders
                1 -> {
                    Slider(
                        value = contrast,
                        onValueChange = { changeImgState(imgState.copy(contrast = it)) },
                        valueRange = 0f..2f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primaryContainer,
                            activeTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                2 -> {
                    Slider(
                        value = brightness,
                        onValueChange = { changeImgState(imgState.copy(brightness = it)) },
                        valueRange = -100f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primaryContainer,
                            activeTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                3 -> {
                    Slider(
                        value = saturation,
                        onValueChange = { changeImgState(imgState.copy(saturation = it)) },
                        valueRange = 0f..2f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primaryContainer,
                            activeTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                4 -> {
                    Slider(
                        value = sharpen,
                        onValueChange = { changeImgState(imgState.copy(sharpen = it)) },
                        valueRange = 0f..6f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primaryContainer,
                            activeTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    }
}

private fun applyOpenCVFilters(bitmap: Bitmap, contrast: Float, brightness: Float, saturation: Float, sharpen: Float): Bitmap {
    val src = bitmapToMat(bitmap)
    val dst = Mat()

    // CONTRAST + BRIGHTNESS
    src.convertTo(dst, -1, contrast.toDouble(), brightness.toDouble())

    // SAT
    if (saturation != 1f) {
        val hsv = Mat()
        Imgproc.cvtColor(dst, hsv, Imgproc.COLOR_BGR2HSV)   // convert to HSV
        val channels = ArrayList<Mat>()
        Core.split(hsv, channels)   // 0 = hue, 1 = sat, 2 = value
        channels[1].convertTo(channels[1], -1, saturation.toDouble(), 0.0)
        Core.merge(channels, hsv)
        Imgproc.cvtColor(hsv, dst, Imgproc.COLOR_HSV2BGR)       // back to BGR
    }

    // SHARPEN (using wiki's "sharpen" kernel)
    if (sharpen > 0f) {
        val kernel = Mat(3, 3, CvType.CV_32F)
        val center = 1f + (sharpen * 4f)
        val data = floatArrayOf(
            0f, -sharpen, 0f,
            -sharpen, center, -sharpen,
            0f, -sharpen, 0f
        )
        kernel.put(0, 0, data)  // fill kernel with data one by one
        val sharpened = Mat()
        Imgproc.filter2D(dst, sharpened, -1, kernel)
        sharpened.copyTo(dst)
    }

    val resultBitmap = createBitmap(dst.cols(), dst.rows(), bitmap.config!!)
    Utils.matToBitmap(dst, resultBitmap)
    return resultBitmap
}