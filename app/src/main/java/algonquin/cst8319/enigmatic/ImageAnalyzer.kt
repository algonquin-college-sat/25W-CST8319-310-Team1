package algonquin.cst8319.enigmatic

import algonquin.cst8319.enigmatic.data.FieldExtractor
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService


@ExperimentalGetImage class ImageAnalyzer(
    private val labelDetectedCallback: LabelDetectedCallback,
    private val listener: ImageAnalyzerListener
) : ImageAnalysis.Analyzer {

    // ML Kit's TextRecognizer instance, used for detecting text in images
    private var recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // FieldExtractor instance - initialized in recognizeText method
    private lateinit var fieldExtractor: FieldExtractor

    // interface for Activity
    interface LabelDetectedCallback {
        fun onLabelDetected()
    }

    // Status flags
    private var isTextProcessingComplete = false
    private var isBarcodeProcessingComplete = false
    private var isBarcodeProcessing = false
    private var isPaused = false


    // data structures to store recognized text blocks and barcode value
    private var barcodeValue = ""
    private var extractedFields = mutableListOf<String>()
    private lateinit var labelJSON: LabelJSON

    //BarcodeScanner instance
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()
    private val barcodeScanner = BarcodeScanning.getClient(options)

    // getters
    fun getBarcodeValue() : String {return barcodeValue}
    fun getExtractedFields() : List<String> {return extractedFields}

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

                // use FieldExtractor to get all fields
                fieldExtractor = FieldExtractor(visionText.textBlocks)
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
     * Updates the UI with the extracted label data.
     */
    fun outputToUI() {
        if (isTextProcessingComplete && isBarcodeProcessingComplete) {
            listener.onSuccess(labelJSON.toJson())
        }
    }

    private fun detectPostalCode(visionText: Text): Boolean {
        val postalCodeRegex = Regex("""[a-zA-Z][O0-9][a-zA-Z][\\ \\-]{0,1}[O0-9][a-zA-Z][O0-9]""")
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
    }

    fun createLabelJSON() {
        labelJSON = LabelJSON(
            fieldExtractor.getProductType(),
            fieldExtractor.getToAddress(),
            fieldExtractor.getDestPostalCode(),
            fieldExtractor.getTrackPin(),
            barcodeValue,
            fieldExtractor.getFromAddress(),
            fieldExtractor.getProductDimension(),
            fieldExtractor.getProductWeight(),
            fieldExtractor.getProductInstruction(),
            fieldExtractor.getReference())

    }

}