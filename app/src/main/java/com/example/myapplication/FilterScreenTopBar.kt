package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun FilterScreenTopBar(navController: NavController, imgState: FilterScreenImgState, changeImgState: (FilterScreenImgState) -> Unit) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // BACK BUTTON
        IconButton(
            onClick = { navController.popBackStack() }
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // TOGGLE CROP MODE
        Spacer(modifier = Modifier.weight(1f))
        Text("Crop Mode")
        Switch(
            checked = imgState.cropMode,
            onCheckedChange = { enabled ->
                changeImgState(imgState.copy(cropMode = enabled))
            },
            modifier = Modifier.padding(8.dp).height(24.dp)
        )
        Spacer(modifier = Modifier.weight(1f))

        // SETTINGS
        var isExpanded by remember {mutableStateOf(false)}
        IconButton(onClick = {isExpanded = !isExpanded}) {      // toggle isExpanded
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings"
            )
            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = {isExpanded = false}
            ) {
                DropdownMenuItem(
                    text = { Text("Placeholder") },
                    onClick = {}
                )
            }
        }
    }
}