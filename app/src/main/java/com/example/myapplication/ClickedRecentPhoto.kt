package com.example.myapplication

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReadMore
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage

class ClickedPhotoViewModel : ViewModel() {
    var uri by mutableStateOf<Uri?>(null)
}
@Composable
fun ClickedRecentPhoto(cameraContext: Context, navController: NavController, viewModel: ClickedPhotoViewModel = viewModel() ) {
    viewModel.uri?.let {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary)
        ) {
            // TOP ROW
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {

                // DOWNLOAD
                IconButton(
                    onClick = { saveToGallery(cameraContext, viewModel.uri, null) },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Download")
                }

                // EXIT
                IconButton(
                    onClick = { viewModel.uri = null },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = "Close")
                }
            }

            // SHOW SELECTED IMG
            AsyncImage(
                model = viewModel.uri!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )

            // SEND TO FILTER SCREEN
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("filter/${Uri.encode(viewModel.uri.toString())}") },
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomEnd),
                icon = {
                    Icon(Icons.AutoMirrored.Filled.ReadMore, contentDescription = "Apply filters and text recognition")
                },
                text = { Text("Edit & Process Image") }
            )
        }
    }
}