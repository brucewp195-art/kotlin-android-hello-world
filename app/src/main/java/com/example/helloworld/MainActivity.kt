package com.example.helloworld

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var textViewResult: TextView
    private var selectedImageUri: Uri? = null
    private lateinit var tess: TessBaseAPI

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
        
        // This ensures you can long-press, highlight, and copy the text!
        textViewResult.setTextIsSelectable(true)

        val btnPickImage = findViewById<Button>(R.id.btnPickImage)
        val btnExtractText = findViewById<Button>(R.id.btnExtractText)

        // Setup Tesseract when the app starts
        prepareTesseract()

        btnPickImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnExtractText.setOnClickListener {
            selectedImageUri?.let { uri ->
                extractText(uri)
            } ?: run {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun prepareTesseract() {
        try {
            val dir = File(filesDir, "tessdata")
            if (!dir.exists()) dir.mkdir()
            
            // Copy the Arabic data file from assets to local storage
            val trainedData = File(dir, "ara.traineddata")
            if (!trainedData.exists()) {
                assets.open("tessdata/ara.traineddata").use { input ->
                    FileOutputStream(trainedData).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            
            tess = TessBaseAPI()
            // Initialize Tesseract with the Arabic language pack
            tess.init(filesDir.absolutePath, "ara")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun extractText(uri: Uri) {
        try {
            textViewResult.text = "Processing..."
            
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            
            if (bitmap != null) {
                tess.setImage(bitmap)
                val extractedText = tess.utF8Text
                textViewResult.text = if (extractedText.isNotBlank()) extractedText else "No text found in image."
            } else {
                textViewResult.text = "Failed to decode image."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            textViewResult.text = "Error extracting text: ${e.message}"
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up memory when the app closes
        if (::tess.isInitialized) {
            tess.recycle()
        }
    }
}
