package com.example.myapplication

import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import java.io.InputStream
import androidx.core.net.toUri
import androidx.core.graphics.createBitmap
import androidx.lifecycle.viewmodel.compose.viewModel

data class Corners(
    val topLeft: Offset = Offset(400f, 400f),
    val topRight: Offset = Offset(800f, 400f),
    val bottomRight: Offset = Offset(800f, 800f),
    val bottomLeft: Offset = Offset(400f, 800f),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(navController: NavController, imageUri: String?) {
    val uri = imageUri?.toUri()
    val context = LocalContext.current

    val viewModel = viewModel<FilterViewModel>()
    val imgState = viewModel.imgState
    val corners = viewModel.corners

    LaunchedEffect(uri) {
        uri?.let { viewModel.loadImage(context, it)}
    }

    // COMPOSABLES
    BottomSheetScaffold(
        sheetContent = { FilterScreenBottomSheet(context, imgState) },
        sheetPeekHeight = 155.dp,
        sheetShape = RoundedCornerShape(0.dp),  // no rounded corners
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        topBar = { FilterScreenTopBar(navController,  imgState, changeImgState = viewModel::updateState) }   // viewModel::updateState = viewModel.updateState()
    ) {
        var scale by remember { mutableFloatStateOf(1f) }
        var rotation by remember { mutableFloatStateOf(0f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
            scale *= zoomChange
            rotation += rotationChange/scale
            offset += offsetChange.times(scale)
        }
        Column(
            modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.onSurface)
        ) {
            Box(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onSurface)
                    .then(
                        if (imgState.cropMode) {
                            Modifier    // disable img transformation when cropping
                        } else {
                            Modifier.transformable(state)
                        }
                    )
            ) {
                // IMAGE + CROP BOX
                FilterImageDisplay(imgState, scale, rotation, offset, corners, changeCorners = viewModel::updateCorners, changeCanvasSize = viewModel::updateCanvasSize)

                // REFRESH BUTTON
                if (imgState.cropMode) {
                    FloatingActionButton(
                        onClick = {
                            val croppedBitmap = warpFromUserQuad(
                                bitmap = imgState.filtered!!,
                                canvasSize = viewModel.canvasSize,
                                topLeft = corners.topLeft,
                                topRight = corners.topRight,
                                bottomRight = corners.bottomRight,
                                bottomLeft = corners.bottomLeft
                            )
                            viewModel.updateState(imgState.copy(rotated = croppedBitmap))
                            viewModel.processTextRecognition(croppedBitmap)
                        },
                        modifier = Modifier.padding(16.dp).align(Alignment.BottomEnd)
                    )
                    { Icon(Icons.Default.Refresh, "") }
                }
            }

            // FILTERS BAR
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                FiltersBar(imgState, corners, changeCorners = viewModel::updateCorners, changeImgState = viewModel::updateState)
            }
        }
    }
}


fun getRotationDegrees(inputStream: InputStream): Int {
    val exif = ExifInterface(inputStream)   //metadata
    return when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
}

fun bitmapToMat(bitmap: Bitmap): Mat {
    val mat = Mat()
    Utils.bitmapToMat(bitmap, mat)
    return mat
}

fun matToBitmap(mat: Mat): Bitmap {
    val bitmap = createBitmap(mat.cols(), mat.rows())
    Utils.matToBitmap(mat, bitmap)
    return bitmap
}

fun rotateMat(mat: Mat, degrees: Int): Mat {
    val rotatedMat = Mat()
    when (degrees) {
        90 -> Core.rotate(mat, rotatedMat, Core.ROTATE_90_CLOCKWISE)
        180 -> Core.rotate(mat, rotatedMat, Core.ROTATE_180)
        270 -> Core.rotate(mat, rotatedMat, Core.ROTATE_90_COUNTERCLOCKWISE)
        else -> mat.copyTo(rotatedMat)
    }
    return rotatedMat
}