package algonquin.cst8319.enigmatic

import algonquin.cst8319.enigmatic.databinding.ActivityMainBinding
import algonquin.cst8319.enigmatic.databinding.BottomSheetBinding
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

@ExperimentalGetImage class MainActivity : AppCompatActivity(), ImageAnalyzer.LabelDetectedCallback, ImageAnalyzerListener {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProvider: ProcessCameraProvider

    private lateinit var binding : ActivityMainBinding
    private lateinit var imageAnalyzer: ImageAnalyzer

    private lateinit var textView: TextView
    private lateinit var closeEfab: ExtendedFloatingActionButton

    private lateinit var bottomSheet: View
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    //docScanner stuff
    val documentScannerOptions = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(false)
        .setPageLimit(1) // or 2 if we want to somehow store multiple scans per session
        .setResultFormats(
            GmsDocumentScannerOptions.RESULT_FORMAT_JPEG // or PDF is we want
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

                        android.os.Handler(Looper.getMainLooper()).postDelayed({
                            displayResults(imageUri, imageAnalyzer.extractedFields, imageAnalyzer.barcodeValue)
                        }, 2000)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        textView = findViewById(R.id.textView)
        closeEfab = findViewById(R.id.close_efab)

        // Get the BottomSheet view from layout
        bottomSheet = findViewById(R.id.bottom_sheet_layout)

        // Set up BottomSheetBehavior
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.isDraggable = false

        /*
        val closeEFab: MaterialButton = findViewById(R.id.close_efab)
        closeEFab.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }*/

        textView.movementMethod = ScrollingMovementMethod()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

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
                imageAnalyzer = ImageAnalyzer(binding, this, this)
                val imageAnalysis = imageAnalyzer.createImageAnalysis(cameraExecutor)

                cameraProvider.bindToLifecycle(
                    this as LifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.d("ERROR", e.message.toString())

            }
        }, ContextCompat.getMainExecutor(this))
    }

    private var isScanningInProgress = false

    /**
     * This gets called by the ImageAnalyzer when it detects a label.
     * We can pause/unbind, then launch the Document Scanner flow.
     */
    override fun onLabelDetected() {
        // trying to prevent duplicate launches ¯\_(ツ)_/¯
        if (isScanningInProgress) {
            Log.d("LabelDetectedCallback", "Doc scanner already launched, skipping...")
            return
        }

        Log.d("LabelDetectedCallback", "A label was detected. Launching doc scanner...")

        isScanningInProgress = true

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

    private fun displayResults(image: Uri, extractedFields: List<String>, barcodeValue: String) {
        Log.d("displayResults", "Displaying results: ${extractedFields.joinToString("\n")}")

        // debugging
        isScanningInProgress = false

        // Update UI to show the scanned image and extracted fields
        binding.previewView.visibility = View.GONE
        binding.resultContainer.visibility = View.VISIBLE
        binding.imageView.visibility = View.VISIBLE
        //binding.textView.visibility = View.VISIBLE
        //binding.fabDismiss.visibility = View.VISIBLE

        textView.text = ""
        binding.imageView.setImageURI(image)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        imageAnalyzer.outputToUI()


        /*
        closeEFab.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        } */

        // Add FloatingActionButton for "Dismiss"

        closeEfab.setOnClickListener {
            binding.previewView.visibility = View.VISIBLE
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            binding.resultContainer.visibility = View.GONE
            binding.imageView.visibility = View.GONE
            //binding.textView.visibility = View.GONE
            //binding.fabDismiss.visibility = View.GONE
            startCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onSuccess(result: String) {
        runOnUiThread {
            textView.text = result
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

}
