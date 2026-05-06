package com.example.myapplication

import android.content.Context
import android.widget.Toast
import androidx.camera.core.TorchState
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun CameraScreenTopRow(cameraContext: Context, cameraController: LifecycleCameraController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {

        // FLASH
        val flashIcon = remember {mutableStateOf(Icons.Default.FlashOff)}
        IconButton(
            onClick = { flash(cameraController, cameraContext)
                if (cameraController.cameraInfo?.torchState?.value == TorchState.ON) {
                    flashIcon.value = Icons.Default.FlashOn
                } else {
                    flashIcon.value = Icons.Default.FlashOff
                }
            }
        ) {
            Icon(
                imageVector = flashIcon.value,
                contentDescription = "Toggle flash"
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

private fun flash(controller: LifecycleCameraController, context: Context) {
    controller.cameraInfo?.let { cameraInfo ->

        if (cameraInfo.hasFlashUnit()) {
            if (cameraInfo.torchState.value == TorchState.OFF) {
                controller.cameraControl?.enableTorch(true)
            } else {
                controller.cameraControl?.enableTorch(false)
            }
        } else {
            Toast.makeText(context, "No torch component detected", Toast.LENGTH_SHORT).show()
        }
    }
}