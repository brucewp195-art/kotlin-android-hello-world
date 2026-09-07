package com.example.helloworld

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

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
        val btnPickImage = findViewById<Button>(R.id.btnPickImage)
        val btnExtractText = findViewById<Button>(R.id.btnExtractText)

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

    private fun extractText(uri: Uri) {
        try {
            val image = InputImage.fromFilePath(this, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            textViewResult.text = "Processing..."

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    textViewResult.text = visionText.text
                }
                .addOnFailureListener { e ->
                    textViewResult.text = "Error extracting text: ${e.message}"
                }
        } catch (e: Exception) {
            e.printStackTrace()
            textViewResult.text = "Failed to load image."
        }
    }
}
