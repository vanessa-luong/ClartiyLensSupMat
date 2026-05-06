package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size as openCVSize
import org.opencv.imgproc.Imgproc
import kotlin.coroutines.resumeWithException
import kotlin.math.abs


data class FilterScreenImgState(
    val rotated: Bitmap? = null,
    val filtered: Bitmap? = null,

    val rawOcrText: String = "Loading...",
    val formattedOcrText: String = rawOcrText,
    val firstRawOcrText: String = "#",             // for resetting
    val firstFormattedOcrText: String = firstRawOcrText,

    val cropMode: Boolean = false,

    val selectedFilter: Int = 1,
    val contrast: Float = 1f,
    val brightness: Float = 0f,
    val saturation: Float = 1f,
    val sharpen: Float = 0f
)

class FilterViewModel : ViewModel() {
    var imgState by mutableStateOf(FilterScreenImgState())
        private set
    var canvasSize by mutableStateOf(Size.Zero)
    var corners by mutableStateOf(Corners())
        private set

    private var rotatedMat: Mat? = null
    private val recogniser = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    override fun onCleared() {
        recogniser.close()
        super.onCleared()
    }

    fun loadImage(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            } ?: return@launch

            val mat = bitmapToMat(bitmap)
            val rotation = context.contentResolver.openInputStream(uri)?.use {
                getRotationDegrees(it)
            } ?: 0
            val rotated = rotateMat(mat, rotation)
            rotatedMat = rotated
            val rotatedBitmap = matToBitmap(rotated)

            withContext(Dispatchers.Main) {
                imgState = imgState.copy(
                    rotated = rotatedBitmap,
                    filtered = rotatedBitmap
                )
            }
            processTextRecognition(rotatedBitmap)
        }
    }

    fun processTextRecognition(bitmap: Bitmap?) {
        if (bitmap == null) return

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val result = runTextRecognition(bitmap)

                var text = "No text detected"
                var formatText = text
                if (!result.textBlocks.isEmpty()) {
                    val rotatedBitmap = preprocessRotate(bitmap, result)

                    val secondResult = runTextRecognition(rotatedBitmap)
                    text = secondResult.text
                    formatText = formatText(secondResult)
                }

                withContext(Dispatchers.Main) {
                    imgState = imgState.copy(
                        rawOcrText = text,
                        formattedOcrText = formatText
                    )
                    if (imgState.firstRawOcrText == "#") {  // first run
                        imgState = imgState.copy(
                            firstRawOcrText = text,
                            firstFormattedOcrText = formatText
                        )
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    imgState = imgState.copy(
                        rawOcrText = "OCR failed",
                        formattedOcrText = "OCR failed",
                    )
                }
            }
        }
    }

    private suspend fun runTextRecognition(bitmap: Bitmap): Text {
        val image = InputImage.fromBitmap(bitmap, 0)

        return suspendCancellableCoroutine { cont ->
            recogniser.process(image)
                .addOnSuccessListener { result ->
                    cont.resume(result) { cause, _, _ -> }
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }
    }

    fun updateState(newState: FilterScreenImgState) {
        imgState = newState
    }

    fun updateCanvasSize(newSize: Size) {
        canvasSize = newSize
    }
    fun updateCorners(newCorners: Corners) {
        corners = newCorners
    }
}

// #################################################################################################

private fun preprocessRotate(originalBitmap: Bitmap, visionText: Text): Bitmap {
    var sumAngle = 0f
    var angleCount = 0
    for (block in visionText.textBlocks) {
        for (line in block.lines) {
            val rotation = line.angle
            if (rotation != 0f) {        // skip 0f angles
                sumAngle += rotation
                angleCount += 1
            }
        }
    }
    val avgAngle = if (angleCount == 0) 0f else -(sumAngle/angleCount)

    val matrix = Matrix()
    matrix.postRotate(avgAngle)

    return Bitmap.createBitmap(
        originalBitmap,
        0,
        0,
        originalBitmap.width,
        originalBitmap.height,
        matrix,
        true
    )
}

// #################################################################################################

private fun formatText(detectedText: Text): String {
    var finalText = ""

    val listOfLines = mutableListOf<Text.Line>()    // go through results and get a list of Text.Line
    for (block in detectedText.textBlocks) {
        for (line in block.lines) {
            listOfLines.add(line)
        }
    }

    val table = mutableListOf<MutableList<Text.Line>>()             // [Line, Line, Line, ...], [Line, Line, Line, ...], [Line, Line, Line, ...], ...
    var currentColumn = mutableListOf<Text.Line>()                  // [Line, Line, Line, ...]

    val listOfLineHeights = listOfLines.mapNotNull { it.boundingBox?.height() }.sorted()
    val listOfLineWidths = listOfLines.mapNotNull { it.boundingBox?.width() }.sorted()
    val maxXGap = listOfLineWidths[(listOfLineWidths.size * 0.5).toInt()]*0.6
    val maxYGap = listOfLineHeights[(listOfLineHeights.size * 0.5).toInt()]
    Log.d("GAP", "${maxXGap}, ${maxYGap}")
    //val maxXGap = 200
    //val maxYGap = 60

    val linesOrderedByX = listOfLines.sortedBy { getX(it)}
    val maxWordLen = listOfLines.maxOf { it.text.length }

    for (word in linesOrderedByX) {
        if (currentColumn.isEmpty()) {
            currentColumn.add(word)
        } else {
            val lastX = getX(currentColumn.last())  // highest X in bucket
            if (abs(getX(word) - lastX) <= maxXGap) {
                currentColumn.add(word)
            } else {
                table.add(currentColumn)                //push bucket to list and reset
                currentColumn = mutableListOf(word)
            }
        }
    }
    if (currentColumn.isNotEmpty()) {           // push last bucket
        table.add(currentColumn)
    }


    val maxColumnLen = table.maxOf {it.size}
    var checkPrevious = 0
    for (row in 0 until maxColumnLen) {         // ROW ^v
        var checkedPreviousFlag = false
        val cell0 = table[0].sortedBy { getY(it)}.getOrNull(row)        // anchor

        for (column in 0 until table.size) {    // COLUMN <->

            Log.d("FINALTEXT", "############ row/I:${row}, column/<->:${column} ")
            val sortedColumn = table[column].sortedBy { getY(it)}
            var appended = false

            for (i in 0..checkPrevious) {       //checks current cell and upwards
                sortedColumn.getOrNull(row-i)?.let { candidate ->       // safe get, no need to limit checkPrevious -> i
                    cell0?.let {
                        Log.d("FINALTEXT", "${checkPrevious}, ${getY(candidate) - getY(cell0)}; row:${row-i}, [${cell0.text}], [${candidate.text}]")
                        if (abs(getY(candidate) - getY(cell0)) <= maxYGap) {
                            finalText += candidate.text + blankSpace(candidate, maxWordLen)
                            appended = true
                            Log.d("FINALTEXT", "...........${getY(candidate) - getY(cell0)}, ${candidate.text}, APPENDED")

                            break   // move onto next column (right cell) once the skipped item has been found
                        }
                    }
                }
            }
            if (!appended) {        // if the current column did not have a match (the item belongs to another row), mark as blank and say we should check behind later
                finalText += blankSpace(sortedColumn.getOrNull(row), maxWordLen)//"[ BLANK] "
                checkedPreviousFlag = true
            }
        }
        if (checkedPreviousFlag) {      // stop checking behind if a row did not have a blank
            checkPrevious += 1
        } else {
            checkPrevious = 0
        }
        finalText += "\n"
    }

    return finalText
}
private fun getX(line: Text.Line): Int{
    return line.boundingBox!!.centerX()
}
private fun getY(line: Text.Line): Int{
    return line.boundingBox!!.centerY()
}
private fun blankSpace(currentWord: Text.Line?, maxWordLen: Int): String {
    if (currentWord == null) {
        return ""
    } else {
        val diff = maxWordLen - currentWord.text.length
        return " ".repeat(diff + 5)
    }
}

// #################################################################################################

fun warpFromUserQuad(bitmap: Bitmap, canvasSize: Size, topLeft: Offset, topRight: Offset, bottomRight: Offset, bottomLeft: Offset): Bitmap {
    val srcMat = bitmapToMat(bitmap)

    val topL = toBitmapCoords(topLeft, canvasSize, bitmap)
    val topR = toBitmapCoords(topRight, canvasSize, bitmap)
    val bottomR = toBitmapCoords(bottomRight, canvasSize, bitmap)
    val bottomL = toBitmapCoords(bottomLeft, canvasSize, bitmap)
    val srcPoints = MatOfPoint2f( topL, topR, bottomR, bottomL)

    val width = (distance(topL, topR) + distance(bottomL, bottomR)) / 2.0
    val height = (distance(topL, bottomL) + distance(topR, bottomR)) / 2.0

    val dstPoints = MatOfPoint2f(
        Point(0.0, 0.0),
        Point(width - 1, 0.0),
        Point(width - 1, height - 1),
        Point(0.0, height - 1)
    )

    val transform = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)

    val outputMat = Mat()
    Imgproc.warpPerspective(
        srcMat,
        outputMat,
        transform,
        openCVSize(width, height)
    )

    return matToBitmap(outputMat)
}

fun toBitmapCoords(point: Offset, canvasSize: Size, bitmap: Bitmap): Point {
    val scaleX = bitmap.width / canvasSize.width
    val scaleY = bitmap.height / canvasSize.height

    return Point(
        (point.x * scaleX).toDouble(),
        (point.y * scaleY).toDouble()
    )
}

fun distance(a: Point, b: Point): Double {
    val dx = (a.x - b.x)
    val dy = (a.y - b.y)
    return kotlin.math.sqrt(dx * dx + dy * dy)
}
