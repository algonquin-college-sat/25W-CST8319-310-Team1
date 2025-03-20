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
    }

    /**
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
            detectLabel(image)
        }
        imageProxy.close()
    }

    /**
     * Detects a shipping label from the given InputImage using ML Kit's TextRecognizer.
     * If a postal code is found within the recognized text, the scanning process is paused,
     * and a callback is triggered to notify the Activity.
     * @param image The InputImage to be processed for label detection.
     */
    private fun detectLabel(image: InputImage) {
        recognizer.process(image)
            .addOnSuccessListener { visionText ->

                isTextProcessingComplete = false

                val isLabelDetected = detectPostalCode(visionText)
                if (isLabelDetected) {
                    // Pause further analysis
                    isPaused = true
                    // Notify the Activity
                    labelDetectedCallback.onLabelDetected()
                }
                Log.i("Label", "Shipping Label detected")
            }
            .addOnFailureListener { e ->
                //Log.e("OCR", "Text recognizer failed: ${e.localizedMessage}", e)
            }
            .addOnCompleteListener {
                // Mark text processing as complete
                isTextProcessingComplete = true
            }
    }
    /**
     * Uses ML Kit's TextRecognizer to detect and process text from the given InputImage.
     * @param image The InputImage to process for text detection.
     * @param onComplete Callback invoked when text recognition is complete, providing
      * a list of extracted field strings.
     */
    private fun recognizeText(image: InputImage, onComplete: (List<String>) -> Unit) {
        isTextProcessingComplete = false

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                Log.d("OCR", "Full detected text: ${visionText.text}")
                // clear list of extracted fields from previous image
                extractedFields.clear()
                // use FieldExtractor to get all fields
                fieldExtractor = FieldExtractor(visionText.textBlocks)
                extractedFields = fieldExtractor.extractAllFields()
                Log.d("OCR", extractedFields.toString())

                onComplete(extractedFields)
            }
            .addOnFailureListener { e ->
                Log.e("OCR", "Text recognizer failed: ${e.localizedMessage}", e)
                // avoids null handling. safer than returning null.
                onComplete(emptyList())
            }
            .addOnCompleteListener {
                // Mark text processing as complete
                isTextProcessingComplete = true
            }
    }


    /**
     * Processes barcode scanning on the given [InputImage].
     *
     * If a barcode scan is already in progress, this function will skip processing
     * the current frame to avoid overlapping scans, and immediately invoke [onComplete]
     * to ensure that the calling flow can continue without hanging.
     *
     * The provided [onComplete] callback will be called after barcode scanning completes
     * successfully, fails, or is skipped.
     *
     * @param image The [InputImage] to scan for barcodes.
     * @param onComplete Callback invoked when barcode processing is finished or skipped.
     */
    private fun processBarcode(image: InputImage, onComplete: () -> Unit) {
        if (isBarcodeProcessing) {
            Log.d("Barcode", "Barcode processing already in progress; skipping this frame.")
            onComplete() // call it to prevent hanging :)
            return
        }

        isBarcodeProcessing = true
        isBarcodeProcessingComplete = false

        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodeValue = ""
                if (barcodes.isNotEmpty()) {
                    for (barcode in barcodes) {
                        barcodeValue = barcode.displayValue ?: ""
                        Log.d("Barcode", "Detected barcode: $barcodeValue")
                    }
                } else {
                   // Log.d("Barcode", "No barcode detected in this frame.")
                }
            }
            .addOnFailureListener { e ->
               // Log.e("Barcode", "Barcode scanning failed: ${e.localizedMessage}", e)
            }
            .addOnCompleteListener {
                // Reset flag so next frame can trigger barcode scanning
                isBarcodeProcessing = false
                // Mark barcode processing as complete
                isBarcodeProcessingComplete = true
                onComplete()
            }
    }


    /**
     * Updates the UI with the extracted label data.
     */
    fun outputToUI() {
            val validate = ValidateData()
            listener.onSuccess(validate.validateAndConvert(labelJSON))

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

    fun analyzeImage(image: InputImage, onComplete: () -> Unit) {
        recognizeText(image) { extractedFields ->
            // Once text recognition is done, process barcode
            processBarcode(image) {
                // Barcode is done — now call onComplete
                onComplete()
            }
        }
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