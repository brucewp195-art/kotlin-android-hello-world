package com.example.helloworld

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var textViewResult: TextView
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            imageView.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        textViewResult = findViewById(R.id.textViewResult)
        textViewResult.setTextIsSelectable(true)

        val btnPickImage = findViewById<Button>(R.id.btnPickImage)
        val btnExtractText = findViewById<Button>(R.id.btnExtractText)

        prepareTesseract()

        btnPickImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnExtractText.setOnClickListener {
            selectedImageUri?.let { uri ->
                extractTextAsync(uri)
            } ?: run {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun prepareTesseract() {
        try {
            val dir = File(filesDir, "tessdata")
            if (!dir.exists()) dir.mkdir()

            val languages = arrayOf("ara", "eng")
            for (lang in languages) {
                val trainedData = File(dir, "$lang.traineddata")
                if (!trainedData.exists()) {
                    assets.open("tessdata/$lang.traineddata").use { input ->
                        FileOutputStream(trainedData).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Downscale large images so OCR doesn't choke on full camera resolution
    private fun downscaleBitmap(bitmap: Bitmap, maxDimension: Int = 1600): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / maxOf(width, height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun extractTextAsync(uri: Uri) {
        textViewResult.text = "Processing..."

        lifecycleScope.launch {
            val result = withContext(Dispatchers.Default) {
                var tess: TessBaseAPI? = null
                try {
                    val inputStream: InputStream? = contentResolver.openInputStream(uri)
                    val rawBitmap = BitmapFactory.decodeStream(inputStream)
                        ?: return@withContext "Failed to decode image."

                    val bitmap = downscaleBitmap(rawBitmap)

                    tess = TessBaseAPI()
                    tess.init(filesDir.absolutePath, "eng+ara")
                    tess.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
                    tess.setImage(bitmap)

                    val extractedText = tess.utF8Text
                    if (extractedText.isNullOrBlank()) "No text found in image." else extractedText
                } catch (e: Exception) {
                    e.printStackTrace()
                    "Error extracting text: ${e.message}"
                } finally {
                    tess?.recycle()
                }
            }
            textViewResult.text = result
        }
    }
}
