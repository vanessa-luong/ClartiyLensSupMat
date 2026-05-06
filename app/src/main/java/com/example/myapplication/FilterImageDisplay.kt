package com.example.myapplication

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun FilterImageDisplay(imgState: FilterScreenImgState, scale: Float, rotation: Float, offset: Offset, corners: Corners, changeCorners: (Corners) -> Unit, changeCanvasSize: (Size) -> Unit) {

    Box(modifier = Modifier
        .graphicsLayer(
            scaleX = if (imgState.cropMode) 1f else scale,
            scaleY = if (imgState.cropMode) 1f else scale,
            rotationZ = if (imgState.cropMode) 0f else rotation,
            translationX = if (imgState.cropMode) 0f else offset.x,
            translationY = if (imgState.cropMode) 0f else offset.y
        )
    ) {

        // IMAGE
        imgState.filtered?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null
            )
        }

        // CROP BOX
        if (imgState.cropMode) {
            Canvas(
                modifier = Modifier.fillMaxSize().matchParentSize()
                    .onSizeChanged {
                        changeCanvasSize(Size(it.width.toFloat(), it.height.toFloat()))
                    }) {
                val path = Path().apply {
                    moveTo(corners.topLeft.x, corners.topLeft.y)
                    lineTo(corners.topRight.x, corners.topRight.y)
                    lineTo(corners.bottomRight.x, corners.bottomRight.y)
                    lineTo(corners.bottomLeft.x, corners.bottomLeft.y)
                    close()
                }

                drawPath(path, Color.White, style = Stroke(width = 3f))
            }
            val latestCorners by rememberUpdatedState(corners)


            DraggableHandle(corners.topLeft) { changeCorners(latestCorners.copy(topLeft = it)) }
            DraggableHandle(corners.topRight) { changeCorners(latestCorners.copy(topRight = it)) }
            DraggableHandle(corners.bottomRight) { changeCorners(latestCorners.copy(bottomRight = it)) }
            DraggableHandle(corners.bottomLeft) { changeCorners(latestCorners.copy(bottomLeft = it)) }
        }
    }
}

@Composable
private fun DraggableHandle(
    position: Offset,
    onPositionChange: (Offset) -> Unit
) {
    var current by remember(position) { mutableStateOf(position) }

    Box(
        modifier = Modifier
            .offset { IntOffset(
                (position.x - 32.dp.toPx()).roundToInt(),        // pixels
                (position.y - 32.dp.toPx()).roundToInt()
            ) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    current += dragAmount
                    onPositionChange(current)
                }
            }
            .size(64.dp)
    )
}
