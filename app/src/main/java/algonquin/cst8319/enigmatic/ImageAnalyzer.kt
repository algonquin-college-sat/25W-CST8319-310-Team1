package algonquin.cst8319.enigmatic

import algonquin.cst8319.enigmatic.data.FieldExtractor
import algonquin.cst8319.enigmatic.databinding.ActivityMainBinding
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
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean



@ExperimentalGetImage class ImageAnalyzer(private var bindingMain: ActivityMainBinding,
                                          private val labelDetectedCallback: ImageAnalyzer.LabelDetectedCallback,
                                          private val listener: ImageAnalyzerListener) : ImageAnalysis.Analyzer {

    // FieldExtractor instance - initialized in recognizeText method
    private lateinit var fieldExtractor: FieldExtractor

    // interface for Activity
    interface LabelDetectedCallback {
        fun onLabelDetected()
    }


    // ML Kit components
    // ML Kit's TextRecognizer instance, used for detecting text in images
    private var recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    //BarcodeScanner instance
    private val barcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
    )

    // Added logging to track performance
    // Logging
    private val logger = KotlinLogging.logger {}

    // Coroutine scope
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

// Added thread safety with AtomicBoolean
// Changed regular boolean flags to AtomicBoolean for better thread safety
// This prevents weird bugs when multiple operations happen at the same time

    // State flags - using AtomicBoolean for thread safety
    private val isTextProcessingComplete = AtomicBoolean(false)
    private val isBarcodeProcessingComplete = AtomicBoolean(false)
    private val isBarcodeProcessing = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)


//    // Status flags
//    private var isTextProcessingComplete = false
//    private var isBarcodeProcessingComplete = false
//    private var isBarcodeProcessing = false
//    private var isSnackbarVisible = false
//    var isPaused = false

    // Results - data structures to store recognized text blocks and barcode value
    var barcodeValue = ""
    var extractedFields = mutableListOf<String>()
    private lateinit var labelJSON: LabelJSON
    // Status flags
//    private var isTextProcessingComplete = false
//    private var isBarcodeProcessingComplete = false
//    private var isBarcodeProcessing = false
//    private var isPaused = false

    // getters
    fun getBarcodeValue(): String {
        return barcodeValue
    }

    fun getExtractedFields(): List<String> {
        return extractedFields
    }

    /**
     * Creates an ImageAnalysis use case with the desired settings and analyzer.
     * @param cameraExecutor The executor used to process image frames in the background.
     * @return The configured ImageAnalysis use case.
     */
    fun createImageAnalysis(cameraExecutor: ExecutorService): ImageAnalysis {
        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Keeps only the latest frame
            .build().apply {

// Changed from using a lambda to directly using this class
                setAnalyzer(cameraExecutor, this@ImageAnalyzer)
//                setAnalyzer(cameraExecutor) { imageProxy ->
//                    analyze(imageProxy) // Send the frame to ML Kit
//                    // the closing of the image proxy was moved to the analyze() function below
//                    // imageProxy.close()
//                }

            }
        return imageAnalyzer
    }

    /**
     * This method is called for every frame that the ImageAnalysis use case processes.
     * Converts the ImageProxy to an InputImage and performs text recognition on it.
     * @param imageProxy The camera frame to analyze.
     */
    override fun analyze(imageProxy: ImageProxy) {

        val startTime = System.currentTimeMillis()
        logger.debug { "FRAME analysis started at $startTime ms" }

        if (isPaused.get()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            analyzeImageConcurrently(image)

        }
        val endTime = System.currentTimeMillis()
        logger.debug { "FRAME analysis completed in ${endTime - startTime} ms" }
        imageProxy.close()
    }

    fun analyzeImageConcurrently(image: InputImage) {
        val startTime = System.currentTimeMillis()
        logger.debug { "Concurrent image analysis started at $startTime ms" }

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
                    logger.debug { "Concurrent image analysis completed in ${endTime - startTime} ms" }
                }
            } catch (e: Exception) {
                logger.debug { "Error in concurrent processing: ${e.message}" }
            }
        }
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


    /**
     * Uses ML Kit's TextRecognizer to detect and process text from the given InputImage.
     * @param image The InputImage to process for text detection.
     * @return list of recognized text blocks
     */
    private suspend fun recognizeTextConcurrently(image: InputImage): MutableList<String> =
        suspendCancellableCoroutine { continuation ->
            val startTime = System.currentTimeMillis()
            logger.debug { "Concurrent text recognition started at $startTime ms" }
// Created a local variable instead of directly using the class-level extractedFields variable
// This approach works better with coroutines and makes our code more predictable when multiple things are happening at once
            val localExtractedFields = mutableListOf<String>()

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val processingTime = System.currentTimeMillis() - startTime
                    logger.debug { "Concurrent text recognition success in $processingTime ms" }
// This line was deleted because isTextProcessingComplete is set to false at the beginning of our analysis process
// and I don't think we should set it to false in the success handler
//                isTextProcessingComplete = false

                    //docscanner stuff
                    if (detectPostalCode(visionText)) {
                        isPaused.set(true)
                        labelDetectedCallback.onLabelDetected()
                    }

                    Log.d("OCR", "Full detected text: ${visionText.text}")

//                // clear list of extracted fields from previous image
//                extractedFields.clear()

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
                    localExtractedFields.addAll(fieldExtractor.extractAllFields())
//                extractedFields = fieldExtractor.extractAllFields()
                    Log.d("OCR", localExtractedFields.toString())

                }
                .addOnFailureListener { e ->
                    Log.e("OCR", "Text recognizer failed: ${e.localizedMessage}", e)
                    val processingTime = System.currentTimeMillis() - startTime
                    logger.debug { "TEXT recognition failed in $processingTime ms: ${e.localizedMessage}" }
// We still need to resume the continuation even when there's an error
// This ensures our app continues running instead of getting stuck waiting forever
                    if (continuation.isActive) {
                        continuation.resume(localExtractedFields)
                    }
                }

                .addOnCompleteListener {
                    val processingTime = System.currentTimeMillis() - startTime
                    logger.debug { "TEXT recognition completed in $processingTime ms" }
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
    private suspend fun processBarcodeConcurrently(image: InputImage): String =
        suspendCancellableCoroutine { continuation ->
            val startTime = System.currentTimeMillis()
            logger.debug { "BARCODE processing started at $startTime ms" }

            var localBarcodeValue = ""

            if (isBarcodeProcessing.getAndSet(true)) {
                Log.d("Barcode", "Barcode  processing already in progress; skipping this frame.")
                continuation.resume("")
                return@suspendCancellableCoroutine
            }
//        isBarcodeProcessing = true
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val processingTime = System.currentTimeMillis() - startTime
                    logger.debug { "Concurrent barcode processing success in $processingTime ms" }

//                barcodeValue = ""
                    if (barcodes.isNotEmpty()) {
                        for (barcode in barcodes) {
                            localBarcodeValue = barcode.displayValue ?: ""
                            Log.d("Barcode", "Detected barcode: $localBarcodeValue")
                            break;
                        }
                    } else {
                        Log.d("Barcode", "No barcode detected in this frame.")
                    }

                }
                .addOnFailureListener { e ->
                    Log.e("Barcode", "Barcode scanning failed: ${e.localizedMessage}", e)
                    val processingTime = System.currentTimeMillis() - startTime
                    logger.debug { "BARCODE processing failed in $processingTime ms: ${e.localizedMessage}" }
                }
                .addOnCompleteListener {
                    val processingTime = System.currentTimeMillis() - startTime
                    logger.debug { "BARCODE processing completed in $processingTime ms" }
                    isBarcodeProcessing.set(false)
                    isBarcodeProcessingComplete.set(true)
                    if (continuation.isActive) {
                        continuation.resume(localBarcodeValue)
                    }
//                // Reset flag so next frame can trigger barcode scanning
//                isBarcodeProcessing = false
//
//                // Mark barcode processing as complete
//                isBarcodeProcessingComplete = true
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

        // Ensure that UI updates run on the main thread
        //bindingMain.root.post {
        //bindingMain.textView.text = ""

// Early return pattern for cleaner code
        // Only proceed if both processing steps are complete
        if (!isTextProcessingComplete.get() || !isBarcodeProcessingComplete.get()) {
            return

            var text = ""
            for (field in extractedFields) {
                //bindingMain.textView.append(field)
                //bindingMain.textView.append("\n")
                text += field + "\n"
            }
            //bindingMain.textView.append("Barcode: $barcodeValue")
            text += "Barcode: $barcodeValue"
            listener.onSuccess(text)
        }
    }

    /**
     * Get extracted text and barcode and return as a String.
     */
//    fun getOutput(): String {
//        var output = ""
//        if (isTextProcessingComplete && isBarcodeProcessingComplete) {
//            if (barcodeValue.isEmpty()) {
//                output += "No valid barcode detected\n"
//            } else {
//                output += "Barcode: $barcodeValue \n"
//            }
//            if (extractedFields.isEmpty()) {
//                output += "No valid text recognized\n"
//            } else {
//                for (field in extractedFields) {
//                    output += field + "\n"
//                }
//            }
//        }
//        return output
//    }

    /**
     * Displays extracted text and barcode to Snackbar.
     * Snackbar is dismissed when it's dismiss action is clicked.
     */
    /*
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
    }*/

    /**
     * A helper method to process detected text blocks, lines, and elements for further use.
     * @param result The recognized text result from ML Kit.
     */
    fun processTextBlock(result: Text) {
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

    fun getTextRecognizer(): TextRecognizer {

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

//    fun analyzeImage (image: InputImage) {
//        recognizeText(image)
//        processBarcode(image)
//        if (isTextProcessingComplete && isBarcodeProcessingComplete) {
//            if (extractedFields.isNotEmpty() && barcodeValue.isNotEmpty()) {
//                outputToUI()
//            }
//        }
//    }

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
            fieldExtractor.getReference()
        )

    }
}