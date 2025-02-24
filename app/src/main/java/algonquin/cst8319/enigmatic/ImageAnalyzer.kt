package algonquin.cst8319.enigmatic

import algonquin.cst8319.enigmatic.data.FieldExtractor
import algonquin.cst8319.enigmatic.databinding.ActivityMainBinding
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


@ExperimentalGetImage class ImageAnalyzer(private var bindingMain: ActivityMainBinding,
                                          private val labelDetectedCallback: LabelDetectedCallback) : ImageAnalysis.Analyzer {
    // ML Kit's TextRecognizer instance, used for detecting text in images
    private var recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // interface for Activity
    interface LabelDetectedCallback {
        fun onLabelDetected()
    }

    // Status flags
    private var isTextProcessingComplete = false
    private var isBarcodeProcessingComplete = false
    private var isBarcodeProcessing = false
    private var isSnackbarVisible = false
    var isPaused = false


    // data structures to store recognized text blocks and barcode value
    var barcodeValue = ""
    var extractedFields = mutableListOf<String>()

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
        if (isPaused) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            analyzeImage(image)

//            recognizeText(image)
//            processBarcode(image)

            if (recognizedTextBlocks.isNotEmpty() || barcodeValue.isNotEmpty()) {
                listener.onSuccess(outputToUI())
                //Thread.sleep(1000)
            }

            // output to TextView now called from within the snackbar dismiss code block
            // outputToUI()

//            if (isTextProcessingComplete && isBarcodeProcessingComplete) {
//                if (extractedFields.isNotEmpty() && barcodeValue.isNotEmpty() && !isSnackbarVisible) {
//                    outputToSnackbar(getOutput())
//                }
//            }

            // loop to suspend the image analyzer until the snackbar is dismissed
//            while (isSnackbarVisible)
//                Thread.sleep(1000)
//            imageProxy.close()
        }
        imageProxy.close()
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

                isTextProcessingComplete = false

                //docscanner stuff
                val isLabelDetected = detectPostalCode(visionText)
                if (isLabelDetected) {
                    // Pause further analysis
                    isPaused = true
                    // Notify the Activity
                    labelDetectedCallback.onLabelDetected()
                }

                Log.d("OCR", "Full detected text: ${visionText.text}")

                // clear list of extracted fields from previous image
                extractedFields.clear()

                // iterating through blocks/lines now deferred to the FieldExtractor
//                for (block in visionText.textBlocks) {
//
//                    val boundingBox = block.boundingBox
//                    val cornerPoints = block.cornerPoints
//                    val text = block.text
//
//                    // logging of each block if necessary
//                    // Log.d("OCR", "Detected text block: ${block.text}")
//
//                    for (line in block.lines) {
//
//                        // re-enable logging of each line if necessary
//                        // Log.d("OCR", "Line text: ${line.text}")
//
//                        for (element in line.elements) {
//
//                            // re-enable logging of each line element if necessary
//                            // Log.d("OCR", "Element text: ${element.text}")
//                        }
//                    }
//                }

                val fieldExtractor = FieldExtractor(visionText.textBlocks)
                extractedFields = fieldExtractor.extractAllFields()
                Log.d("OCR", extractedFields.toString())

            }
            .addOnFailureListener { e ->
                //Log.e("OCR", "Text recognizer failed: ${e.localizedMessage}", e)
            }
            .addOnCompleteListener {
                // Mark text processing as complete
                isTextProcessingComplete = true
            }

        return extractedFields

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
                //Log.e("Barcode", "Barcode scanning failed: ${e.localizedMessage}", e)
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
    fun outputToUI() {
        // Ensure that UI updates run on the main thread
        bindingMain.root.post {
            bindingMain.textView.text = ""
            if (isTextProcessingComplete && isBarcodeProcessingComplete) {
                for (field in extractedFields) {
                    bindingMain.textView.append(field)
                    bindingMain.textView.append("\n")
                }
                bindingMain.textView.append("Barcode: $barcodeValue")
            }
        }*/

        var output : String = ""

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
     * Get extracted text and barcode and return as a String.
     */
    fun getOutput(): String {
        var output = ""
        if (isTextProcessingComplete && isBarcodeProcessingComplete) {
            if (barcodeValue.isEmpty()) {
                output += "No valid barcode detected\n"
            } else {
                output += "Barcode: $barcodeValue \n"
            }
            if (extractedFields.isEmpty()) {
                output += "No valid text recognized\n"
            } else {
                for (field in extractedFields) {
                    output += field + "\n"
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
            bindingMain.textView.text = extract
            extractedFields.clear()
            barcodeValue = ""
            snackbar.dismiss()
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

    fun detectPostalCode(visionText: Text): Boolean {
        val postalCodeRegex = Regex("[A-Za-z]\\d[A-Za-z]\\s?\\d[A-Za-z]\\d")
        // Iterate over all text blocks, lines, or elements
        for (block in visionText.textBlocks) {
            val blockText = block.text
            // Check if this block contains a valid Canadian postal code
            if (postalCodeRegex.containsMatchIn(blockText)) {
                return true // Found at least one match
            }
        }
        return false // No match found
    }

    fun analyzeImage (image: InputImage) {
        recognizeText(image)
        processBarcode(image)

        if (isTextProcessingComplete && isBarcodeProcessingComplete) {
            if (extractedFields.isNotEmpty() && barcodeValue.isNotEmpty()) {
                outputToUI()
            }
        }

    }



}
