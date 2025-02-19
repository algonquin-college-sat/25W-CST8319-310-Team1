package algonquin.cst8319.enigmatic

import algonquin.cst8319.enigmatic.databinding.ActivityMainBinding
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService


@ExperimentalGetImage class ImageAnalyzer(private var bindingMain: ActivityMainBinding) : ImageAnalysis.Analyzer {
    // ML Kit's TextRecognizer instance, used for detecting text in images
    private var recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Status flags
    private var isTextProcessingComplete = false
    private var isBarcodeProcessingComplete = false
    private var isBarcodeProcessing = false
    private var isSnackbarVisible = false


    // data structures to store recognized text blocks and barcode value
    private val recognizedTextBlocks = mutableListOf<String>()
    private var barcodeValue = ""

    //BarcodeScanner instance
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()
    private val barcodeScanner = BarcodeScanning.getClient(options)

    /**
     * Creates an ImageAnalysis use case with the desired settings and analyzer.
     * @param cameraExecutor The executor used to process image frames in the background.
     * @return The configured ImageAnalysis use case.
     */
    fun createImageAnalysis(cameraExecutor: ExecutorService): ImageAnalysis {
        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Keeps only the latest frame
            .build().apply {
                setAnalyzer(cameraExecutor) { imageProxy ->
                    analyze(imageProxy) // Send the frame to ML Kit
                    // the closing of the image proxy was moved to the analyze() function below
                    // imageProxy.close()
                }
            }
        return imageAnalyzer
    } /**
     * This method is called for every frame that the ImageAnalysis use case processes.
     * Converts the ImageProxy to an InputImage and performs text recognition on it.
     * @param imageProxy The camera frame to analyze.
     */
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            recognizeText(image)
            processBarcode(image)

            // output to TextView now called from within the snackbar dismiss code block
            // outputToUI()

//            if (isTextProcessingComplete && isBarcodeProcessingComplete) {
                if (recognizedTextBlocks.isNotEmpty() && barcodeValue.isNotEmpty() && !isSnackbarVisible) {
                    outputToSnackbar(getOutput())
                }
//            }

            // loop to suspend the image analyzer until the snackbar is dismissed
            while (isSnackbarVisible)
                Thread.sleep(1000)
            imageProxy.close()
        }
    }

    /**
     * Uses ML Kit's TextRecognizer to detect and process text from the given InputImage.
     * @param image The InputImage to process for text detection.
     * @return list of recognized text blocks
     */
    private fun recognizeText(image: InputImage): MutableList<String> {
        this.recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val fromAddressRegion = Rect(100, 470, 220, 515)
        val toAddressRegion = Rect(81, 205, 185, 266)
        val tracking = Rect(202, 388, 302, 483)
        val postalCodeRegion = Rect(87, 294, 190, 341)
        val result = recognizer.process(image)
            .addOnSuccessListener { visionText ->

                isTextProcessingComplete = false

                Log.d("OCR", "Full detected text: ${visionText.text}")

                // clear list of text blocks from previous image
                recognizedTextBlocks.clear()

                for (block in visionText.textBlocks) {
                    val boundingBox = block.boundingBox
                    val cornerPoints = block.cornerPoints
                    val text = block.text

                    // re-enable logging of each block if necessary
                    // Log.d("OCR", "Full detected text: ${block.text}")

                    // add new text blocks to list
                    if (text !in recognizedTextBlocks)
                        recognizedTextBlocks.add(text)

                    for (block in visionText.textBlocks) {
                        val bound = block.boundingBox
                        if (bound != null) {
                            Log.d("OCR_DEBUG", "Detected Text: '${block.text}' at Bounding Box: $bound")

                            when {
                                fromAddressRegion.contains(bound) ->
                                    Log.d("OCR_DEBUG", "✅ 'From' Address detected: ${block.text}, Bounding Box: $bound, Expected Region: $fromAddressRegion")

                                toAddressRegion.contains(bound) ->
                                    Log.d("OCR_DEBUG", "✅ 'To' Address detected: ${block.text}, Bounding Box: $bound, Expected Region: $toAddressRegion")

                                tracking.contains(bound) ->
                                    Log.d("OCR_DEBUG", "✅ Tracking Number detected: ${block.text}, Bounding Box: $bound, Expected Region: $tracking")

                                postalCodeRegion.contains(bound) ->
                                    Log.d("OCR_DEBUG", "✅ Postal Code detected: ${block.text}, Bounding Box: $bound, Expected Region: $postalCodeRegion")

                                else ->
                                    Log.d("OCR_DEBUG", "❌ Text outside defined regions: ${block.text}, Bounding Box: $bound")
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("OCR", "Text recognizer failed: ${e.localizedMessage}", e)
            }
            .addOnCompleteListener {
                // Mark text processing as complete
                isTextProcessingComplete = true

                Log.d("OCR", "recognizedText: $recognizedTextBlocks")

            }

        return recognizedTextBlocks

    }

    /**
     * Processes barcode scanning on the given InputImage.
     */
    private fun processBarcode(image: InputImage) {
        if (isBarcodeProcessing) {
            Log.d("Barcode", "Barcode processing already in progress; skipping this frame.")
            return
        }
        isBarcodeProcessing = true
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodeValue = ""
                if (barcodes.isNotEmpty()) {
                    for (barcode in barcodes) {
                        barcodeValue = barcode.displayValue ?: ""
                        Log.d("Barcode", "Detected barcode: $barcodeValue")

                    }
                } else {
                    Log.d("Barcode", "No barcode detected in this frame.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Barcode", "Barcode scanning failed: ${e.localizedMessage}", e)
            }
            .addOnCompleteListener {
                // Reset flag so next frame can trigger barcode scanning
                isBarcodeProcessing = false

                // Mark barcode processing as complete
                isBarcodeProcessingComplete = true
            }
    }

    /**
     * Updates the UI with the recognized text and barcode value.
     * This method ensures that UI updates are performed on the main thread
     * by using the `post` method of the root view. It first clears any previous
     * content in the `textView`, then appends each recognized text block followed
     * by the barcode value if both text and barcode processing are complete.
     */
    private fun outputToUI() {
        // Ensure that UI updates run on the main thread
        bindingMain.root.post {
            bindingMain.textView.text = ""
            if (isTextProcessingComplete && isBarcodeProcessingComplete) {
                for (block in recognizedTextBlocks) {
                    bindingMain.textView.append(block)
                    bindingMain.textView.append("\n")
                }
                bindingMain.textView.append("Barcode: $barcodeValue")
            }
        }
    }

    /**
     * Get extracted text and barcode and return as a String.
     */
    private fun getOutput(): String {
        var output = ""
        if (isTextProcessingComplete && isBarcodeProcessingComplete) {
            if (barcodeValue.isEmpty()) {
                output += "No valid barcode detected\n"
            } else {
                output += "Barcode: $barcodeValue \n"
            }
            if (recognizedTextBlocks.isEmpty()) {
                output += "No valid text recognized\n"
            } else {
                for (block in recognizedTextBlocks) {
                    output += block + "\n"
                }
            }
        }
        return output
    }

    /**
     * Displays extracted text and barcode to Snackbar.
     * Snackbar is dismissed when it's dismiss action is clicked.
     */
    private fun outputToSnackbar(extract: String) {
        isSnackbarVisible = true

        val snackbar = Snackbar.make(bindingMain.root, extract, Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction("DISMISS") {

            snackbar.dismiss()
            bindingMain.textView.text = extract
            isSnackbarVisible = false
        }
        snackbar.setTextMaxLines(50)
        snackbar.show()
    }

    /**
     * A helper method to process detected text blocks, lines, and elements for further use.
     * @param result The recognized text result from ML Kit.
     */
    private fun processTextBlock(result: Text) {
        // process text block
        val resultText = result.text
        for (block in result.textBlocks) {
            val blockText = block.text
            val blockCornerPoints = block.cornerPoints
            val blockFrame = block.boundingBox
            for (line in block.lines) {
                val lineText = line.text
                val lineCornerPoints = line.cornerPoints
                val lineFrame = line.boundingBox
                for (element in line.elements) {
                    val elementText = element.text
                    val elementCornerPoints = element.cornerPoints
                    val elementFrame = element.boundingBox
                }
            }
        }

    }

    private fun getTextRecognizer(): TextRecognizer {

        return TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    }



}
