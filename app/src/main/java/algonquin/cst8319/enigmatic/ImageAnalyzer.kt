package algonquin.cst8319.enigmatic

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService

@ExperimentalGetImage class ImageAnalyzer : ImageAnalysis.Analyzer {
    // ML Kit's TextRecognizer instance, used for detecting text in images
    private var recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
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
        }
    }
    /**
     * Uses ML Kit's TextRecognizer to detect and process text from the given InputImage.
     * @param image The InputImage to process for text detection.
     */
    private fun recognizeText(image: InputImage) {


        this.recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val result = recognizer.process(image)
            .addOnSuccessListener { visionText ->


                for (block in visionText.textBlocks) {
                    val boundingBox = block.boundingBox
                    val cornerPoints = block.cornerPoints
                    val text = block.text
                    Log.d("OCR", "Full detected text: ${visionText.text}")

                    for (line in block.lines) {

                        for (element in line.elements) {

                            Log.d("OCR", "Line text: ${line.text}")
                        }
                    }
                }

            }
            .addOnFailureListener { e ->

            }

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
