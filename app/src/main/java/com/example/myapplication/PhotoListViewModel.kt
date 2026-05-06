package com.example.myapplication

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PhotoItem(
    val bitmap: Bitmap,
    val uri: Uri
)
class PhotoListViewModel: ViewModel() {
    private val _photos = MutableStateFlow<List<PhotoItem>>(emptyList())    //flow = asynch, thread safe
    val photos = _photos.asStateFlow()

    fun onTakePhoto(bitmap: Bitmap, uri: Uri) {
        _photos.value += PhotoItem(bitmap, uri)

    }
}