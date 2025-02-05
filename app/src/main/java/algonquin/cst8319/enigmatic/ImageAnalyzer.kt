package algonquin.cst8319.enigmatic

import algonquin.cst8319.enigmatic.databinding.ActivityMainBinding
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode


@ExperimentalGetImage class ImageAnalyzer(private var bindingMain: ActivityMainBinding) : ImageAnalysis.Analyzer {
    // ML Kit's TextRecognizer instance, used for detecting text in images
    private var recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Processing flags
    private var isTextProcessingComplete = false
    private var isBarcodeProcessingComplete = false

    // data structures to store recognized text blocks and barcode value
    private val recognizedTextBlocks = mutableListOf<String>()
    private var barcodeValue = ""

    //BarcodeScanner instance
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()
    private val barcodeScanner = BarcodeScanning.getClient(options)

    private var isSnackbarVisible = false

    /**
     * Creates an ImageAnalysis use case with the desired settings and analyzer.
     * @param cameraExecutor The executor used to process image frames in the background.
     * @return The configured ImageAnalysis use case.
     */
    fun createImageAnalysis(cameraExecutor: ExecutorService): ImageAnalysis {
        var imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Keeps only the latest frame
            .build().apply {
                setAnalyzer(cameraExecutor) { imageProxy ->
                    analyze(imageProxy) // Send the frame to ML Kit
                    imageProxy.close()
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
            // outputToUI()

            if (getOutput() != "" && !isSnackbarVisible) {
                    outputToSnackbar(getOutput())
            }
        }
    }

    /**
     * Uses ML Kit's TextRecognizer to detect and process text from the given InputImage.
     * @param image The InputImage to process for text detection.
     * @return list of recognized text blocks
     */
    private fun recognizeText(image: InputImage): MutableList<String> {
        this.recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val result = recognizer.process(image)
            .addOnSuccessListener { visionText ->

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

                    for (line in block.lines) {

                        // re-enable logging of each line if necessary
                        // Log.d("OCR", "Line text: ${line.text}")

                        for (element in line.elements) {

                            // re-enable logging of each line element if necessary
                            // Log.d("OCR", "Element text: ${element.text}")
                        }
                    }
                }

                // 2-second pause between each successful text recognition
                Thread.sleep(2000)

            }
            .addOnFailureListener { e ->

            }
            .addOnCompleteListener {
                // Mark text processing as complete
                isTextProcessingComplete = true
            }

        return recognizedTextBlocks

    }

    /**
     * Processes barcode scanning on the given InputImage.
     * This method is called only when isBarcodeExpected() returns true.
     */
    private var isBarcodeProcessing = false
    private fun processBarcode(image: InputImage) {
        if (isBarcodeProcessing) {
            Log.d("Barcode", "Barcode processing already in progress; skipping this frame.")
            return
        }
        isBarcodeProcessing = true
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    for (barcode in barcodes) {
                        barcodeValue = barcode.displayValue ?: ""
                        Log.d("Barcode", "Detected barcode: $barcodeValue")

//                        recognizedTextBlocks.add("Barcode: $barcodeValue")
//                        Log.d("OCR", "recognizedText: $recognizedTextBlocks")
                    }
                } else {
                    Log.d("Barcode", "No barcode detected in this frame.")
                }
            }
            .addOnFailureListener { e ->
                //Log.e("Barcode", "Barcode scanning failed2: ${e.localizedMessage}", e)
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
            for (block in recognizedTextBlocks) {
                output += block
                output += "\n"
            }
            output += "Barcode: $barcodeValue"
        }
        return output
    }

    /**
     * Displays extracted text and barcode to Snackbar.
     * Snackbar is dismissed when it's dismiss action is clicked.
     */
    private fun outputToSnackbar(extract: String) {
        isSnackbarVisible = true

        var snackbar = Snackbar.make(bindingMain.root, extract, Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction("DISMISS") {
            snackbar.dismiss()
            isSnackbarVisible = false
        }
        snackbar.setTextMaxLines(30)
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
