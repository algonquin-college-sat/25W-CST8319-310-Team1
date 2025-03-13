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
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*

import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutorService

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume


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

    // State flags - using AtomicBoolean for thread safety
    private val isTextProcessingComplete = AtomicBoolean(false)
    private val isBarcodeProcessingComplete = AtomicBoolean(false)
    private val isBarcodeProcessing = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)


    // data structures to store recognized text blocks and barcode value
    private var barcodeValue = ""
    private var extractedFields = mutableListOf<String>()
    private lateinit var labelJSON: LabelJSON

    //BarcodeScanner instance
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()
    private val barcodeScanner = BarcodeScanning.getClient(options)

    // Logging
    private val logger = KotlinLogging.logger {}

    // Coroutine scope
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

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
        if (isPaused.get()) {
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

    fun analyzeImage(image: InputImage){
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
// This line was deleted because isTextProcessingComplete is set to false at the beginning of our analysis process
// and I don't think we should set it to false in the success handler
//                isTextProcessingComplete = false

                //docscanner stuff
                val isLabelDetected = detectPostalCode(visionText)
                if (isLabelDetected) {
                    // Pause further analysis
                    isPaused.set(true)
                    // Notify the Activity
                    labelDetectedCallback.onLabelDetected()
                }

            }
            .addOnFailureListener { e ->
                Log.d("OCR", "Postal code recogniztion failed: ${e.localizedMessage}", e)

            }

    }
    fun analyzeImageConcurrently(image: InputImage) {
        val startTime = System.currentTimeMillis()
        logger.debug {"Concurrent image analysis started at $startTime ms"}

        // Reset completion flags
        isTextProcessingComplete.set(false)
        isBarcodeProcessingComplete.set(false)

// Added coroutines for concurrent processing
// Now text recognition and barcode scanning run at the same time
// This makes our app faster because it doesn't wait for one to finish before starting the other
        // Launch coroutines for concurrent processing
        coroutineScope.launch {
            try {
                // Create deferred results for both operations
                val textRecognitionDeferred = async { recognizeTextConcurrently(image) }
                val barcodeProcessingDeferred = async { processBarcodeConcurrently(image) }

                // Await both results
                extractedFields = textRecognitionDeferred.await()
                barcodeValue = barcodeProcessingDeferred.await()

                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    if (extractedFields.isNotEmpty() && barcodeValue.isNotEmpty()) {
                        outputToUI()
                    }

                    val endTime = System.currentTimeMillis()
                    logger.debug {"Concurrent image analysis completed in ${endTime - startTime} ms"}
                }
            } catch (e: Exception) {
                logger.debug {"Error in concurrent processing: ${e.message}"}
            }
        }
    }
    /**
     * Uses ML Kit's TextRecognizer to detect and process text from the given InputImage.
     * @param image The InputImage to process for text detection.
     * @return list of recognized text blocks
     */
    private suspend fun recognizeTextConcurrently(image: InputImage): MutableList<String> = suspendCancellableCoroutine { continuation ->
        val startTime = System.currentTimeMillis()
        logger.debug {"Concurrent text recognition started at $startTime ms"}
// Created a local variable instead of directly using the class-level extractedFields variable
// This approach works better with coroutines and makes our code more predictable when multiple things are happening at once
        val localExtractedFields = mutableListOf<String>()
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
// This line was deleted because isTextProcessingComplete is set to false at the beginning of our analysis process
// and I don't think we should set it to false in the success handler
//                isTextProcessingComplete = false

//                //docscanner stuff
//                val isLabelDetected = detectPostalCode(visionText)
//                if (isLabelDetected) {
//                    // Pause further analysis
//                    isPaused.set(true)
//                    // Notify the Activity
//                    labelDetectedCallback.onLabelDetected()
//                }
//
//                Log.d("OCR", "Full detected text: ${visionText.text}")

// use FieldExtractor to get all fields
//                fieldExtractor = FieldExtractor(visionText.textBlocks)
//                extractedFields = fieldExtractor.extractAllFields()
                fieldExtractor = FieldExtractor(visionText.textBlocks)
                localExtractedFields.addAll(fieldExtractor.extractAllFields())
                Log.d("OCR", localExtractedFields.toString())

            }
            .addOnFailureListener { e ->
                Log.d("OCR", "Text recognizer failed: ${e.localizedMessage}", e)
                val processingTime = System.currentTimeMillis() - startTime
                logger.debug {"TEXT recognition failed in $processingTime ms: ${e.localizedMessage}"}
// We still need to resume the continuation even when there's an error
// This ensures our app continues running instead of getting stuck waiting forever
                if (continuation.isActive) {
                    continuation.resume(localExtractedFields)
                }
            }
            .addOnCompleteListener {
                val processingTime = System.currentTimeMillis() - startTime
                logger.debug {"TEXT recognition completed in $processingTime ms"}
                // Mark text processing as complete
                isTextProcessingComplete.set(true)
// Check if continuation.isActive before resuming to make sure the coroutine hasn't been cancelled
// Then use continuation.resume() to return our results and continue execution
                if (continuation.isActive) {
                    continuation.resume(localExtractedFields)
                }
            }

// Since this func is using the suspendCancellableCoroutine pattern, the function's return value is provided through the continuation mechanism instead of a traditional return statement
        // return extractedFields
// Added cancellation handling
// This makes sure resources are cleaned up if operations are cancelled
        continuation.invokeOnCancellation {
            logger.debug { "Text recognition cancelled" }
        }
    }

    /**
     * Processes barcode scanning on the given InputImage.
     */
    private suspend fun processBarcodeConcurrently(image: InputImage): String = suspendCancellableCoroutine { continuation ->
        val startTime = System.currentTimeMillis()
        logger.debug {"BARCODE processing started at $startTime ms"}

        var localBarcodeValue = ""

        if (isBarcodeProcessing.getAndSet(true)) {
            Log.d("Barcode", "Barcode processing already in progress; skipping this frame.")
            continuation.resume("")
            return@suspendCancellableCoroutine
        }
        // isBarcodeProcessing = true
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                val processingTime = System.currentTimeMillis() - startTime
                logger.debug { "Concurrent barcode processing success in $processingTime ms" }

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
                val processingTime = System.currentTimeMillis() - startTime
                logger.debug {"BARCODE processing failed in $processingTime ms: ${e.localizedMessage}"}
            }
            .addOnCompleteListener {
                val processingTime = System.currentTimeMillis() - startTime
                logger.debug {"BARCODE processing completed in $processingTime ms"}
                // Reset flag so next frame can trigger barcode scanning
                isBarcodeProcessing.set(false)
                // Mark barcode processing as complete
                isBarcodeProcessingComplete.set(true)
                if (continuation.isActive) {
                    continuation.resume(localBarcodeValue)
                }
            }
        continuation.invokeOnCancellation {
            isBarcodeProcessing.set(false)
            logger.debug { "Barcode processing cancelled" }
        }
    }

    /**
     * Updates the UI with the extracted label data.
     */
    fun outputToUI() {
        if (isTextProcessingComplete.get() || !isBarcodeProcessingComplete.get()) {
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
// Added resource cleanup
// Now we close ML Kit resources when we're done
// This prevents memory leaks and makes our app run better
    // Cancel coroutines when the analyzer is no longer needed
    fun shutdown() {
        coroutineScope.cancel()
        recognizer.close()
        barcodeScanner.close()
    }
}