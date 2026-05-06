package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.myapplication.ui.theme.MyApplicationTheme
import org.opencv.android.OpenCVLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // load openCV library
        if (OpenCVLoader.initLocal()) {
            Log.d("LOADED", "OpenCV loaded successfully")
        } else {
            Log.d("LOADED", "OpenCV initialization failed!")
        }

        setContent {
            MyApplicationTheme {
                Navigation()
            }
        }
    }
}