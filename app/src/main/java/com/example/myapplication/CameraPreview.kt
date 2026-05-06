package com.example.myapplication

import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun CameraPreview(controller: LifecycleCameraController, modifier: Modifier = Modifier) {
    // --------------------------------------------------------------------------------------------- from Philip Lackner
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(        // view in compose
        factory = {                                 // create camerax previewView + link to activity
            PreviewView(it).apply {
                this.controller = controller
                controller.bindToLifecycle(lifecycleOwner)
            }
        },
        // -----------------------------------------------------------------------------------------
        modifier = modifier.transformable(rememberTransformableState { zoomChange, _, _ ->    // _offset, _rotation
            val currentZoom = controller.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
            controller.cameraControl?.setZoomRatio(currentZoom * zoomChange)
        })
    )

}