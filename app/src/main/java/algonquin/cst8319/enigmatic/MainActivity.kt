package algonquin.cst8319.enigmatic

import algonquin.cst8319.enigmatic.databinding.ActivityMainBinding
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@ExperimentalGetImage class MainActivity : AppCompatActivity(), ImageAnalyzer.LabelDetectedCallback {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProvider: ProcessCameraProvider

    private lateinit var binding : ActivityMainBinding
    private lateinit var imageAnalyzer: ImageAnalyzer

    //docScanner stuff
    val documentScannerOptions = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(false)
        .setPageLimit(1) // or 2, etc.
        .setResultFormats(
            GmsDocumentScannerOptions.RESULT_FORMAT_JPEG // or PDF or both
        )
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .build()

    // The client that launches the document scanner flow
    val docScannerClient = GmsDocumentScanning.getClient(documentScannerOptions)

    // Define scannerLauncher:
    private val scannerLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
        if (activityResult.resultCode == RESULT_OK) {
            // Retrieve the scanning result
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)

            // For JPEG pages (if you requested RESULT_FORMAT_JPEG)
            if (scanResult != null) {
                scanResult.getPages()?.let { pages ->
                    for ((index, page) in pages.withIndex()) {
                        val imageUri = page.imageUri
                        // Do something with this Uri, e.g. run Text Recognition
                        Log.d("DocScanner", "JPEG page $index => $imageUri")
                        val scannedImage = InputImage.fromFilePath(this,imageUri)
                        imageAnalyzer.analyzeImage(scannedImage)
                    }
                }
            }
        }
        startCamera()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()
        checkCameraPermission()
        // start the camera
        startCamera()
    }
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                1001
            )
        }
    }

    private fun startCamera() {
        val processCameraProvider = ProcessCameraProvider.getInstance(this)

        processCameraProvider.addListener({
            cameraProvider = processCameraProvider.get()

            // Set up the Preview use case
            val preview = Preview.Builder().build().also {
                val previewView = binding.previewView
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            try {
                // Bind the camera to the lifecycle
                cameraProvider.unbindAll()

                // instantiate the ImageAnalyzer and bind it to the cameraProvider
                imageAnalyzer = ImageAnalyzer(binding, this)
                val imageAnalysis = imageAnalyzer.createImageAnalysis(cameraExecutor)

                cameraProvider.bindToLifecycle(
                    this as LifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis
                )
            } catch (e: Exception) {
                Log.d("ERROR", e.message.toString())

            }
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * This gets called by the ImageAnalyzer when it detects a label.
     * We can pause/unbind, then launch the Document Scanner flow.
     */
    override fun onLabelDetected() {
        Log.d("LabelDetectedCallback", "A label was detected. Launching doc scanner...")

        // Unbind (optional) or set a pause flag in the analyzer
        cameraProvider.unbindAll()  // or leave the preview bound if you want
        // 2) Start document scanner
        startDocumentScanner()
    }

    fun startDocumentScanner() {
        docScannerClient.getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e ->
                Log.e("DocScanner", "Failed to launch doc scanner: ${e.message}", e)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}