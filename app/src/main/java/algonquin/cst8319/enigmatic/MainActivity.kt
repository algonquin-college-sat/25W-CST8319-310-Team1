package algonquin.cst8319.enigmatic

import algonquin.cst8319.enigmatic.databinding.ActivityMainBinding
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
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

@ExperimentalGetImage class MainActivity : AppCompatActivity(), ImageAnalyzer.LabelDetectedCallback, ImageAnalyzerListener {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProvider: ProcessCameraProvider

    private lateinit var binding : ActivityMainBinding
    private lateinit var imageAnalyzer: ImageAnalyzer

    private lateinit var textView: TextView
    private lateinit var bottomSheetHeader: TextView
    private lateinit var closeEfab: ExtendedFloatingActionButton
    private lateinit var copyEfab: ExtendedFloatingActionButton

    private lateinit var bottomSheet: View
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private val viewModel: MainActivityViewModel by viewModels<MainActivityViewModel>()

    //docScanner stuff
    private val documentScannerOptions = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(false)
        .setPageLimit(1) // or 2 if we want to somehow store multiple scans per session
        .setResultFormats(
            GmsDocumentScannerOptions.RESULT_FORMAT_JPEG // or PDF is we want
        )
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .build()

    // The client that launches the document scanner flow
    private val docScannerClient = GmsDocumentScanning.getClient(documentScannerOptions)

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
                            // create labelJSON
                            imageAnalyzer.createLabelJSON()
                            // output to UI
                            displayResults(imageUri, imageAnalyzer.getExtractedFields(), imageAnalyzer.getBarcodeValue())
                        }, 1000)
                    }
                }
            }
        }
    }

    // Status flags
    private var isScanningInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the BottomSheet view from layout
        bottomSheet = findViewById(R.id.bottom_sheet_layout)
        textView = findViewById(R.id.textView)
        bottomSheetHeader = findViewById(R.id.bottom_sheet_header)
        closeEfab = findViewById(R.id.close_efab)
        copyEfab = findViewById(R.id.copy_efab)

        // Set up BottomSheetBehavior
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.isDraggable = false
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        textView.movementMethod = ScrollingMovementMethod()
        bottomSheetHeader.text = getString(R.string.scanning)

        // Create the observers which update the UI.
        val textObserver = Observer<String> { newText ->
            textView.text = newText
        }

        val headerObserver = Observer<String> { newText ->
            bottomSheetHeader.text = newText
        }

        val previewViewVisibilityObserver = Observer<Int> { visibility ->
            binding.previewView.visibility = visibility
        }

        val resultContainerVisibilityObserver = Observer<Int> { visibility ->
            binding.resultContainer.visibility = visibility
        }

        val imageViewVisibilityObserver = Observer<Int> { visibility ->
            binding.imageView.visibility = visibility
        }

        // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        viewModel.currentText.observe(this, textObserver)
        viewModel.headerText.observe(this, headerObserver)
        viewModel.previewViewVisibility.observe(this, previewViewVisibilityObserver)
        viewModel.resultContainerVisibility.observe(this, resultContainerVisibilityObserver)
        viewModel.imageViewVisibility.observe(this, imageViewVisibilityObserver)
        viewModel.scannedImage.observe(this) { uri ->
            uri?.let {
                binding.imageView.setImageURI(it)
            }
        }

        // FloatingActionButton for "Close"
        closeEfab.setOnClickListener {
            viewModel.previewViewVisibility.value = View.VISIBLE
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            // Update viewModel
            viewModel.headerText.value = getString(R.string.scanning)
            // Clearing the textView causes the bottom sheet to be hidden on "close".
            // Not necessary while hidden anyways.
            // viewModel.currentText.value = getString(R.string.empty_string)

            viewModel.resultContainerVisibility.value = View.GONE
            viewModel.imageViewVisibility.value = View.GONE

            startCamera()
        }

        // FloatingActionButton for "Copy"
        copyEfab.setOnClickListener {
            
        }

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
                imageAnalyzer = ImageAnalyzer(this, this)
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

    private fun startDocumentScanner() {
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
        viewModel.previewViewVisibility.value = View.GONE
        viewModel.resultContainerVisibility.value = View.VISIBLE
        viewModel.imageViewVisibility.value = View.VISIBLE
        viewModel.setScannedImage(image)

        textView.text = getString(R.string.empty_string)
        binding.imageView.setImageURI(image)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        imageAnalyzer.outputToUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onSuccess(result: String) {
        runOnUiThread {
            bottomSheetHeader.text = getString(R.string.label_information)
            textView.text = getString(R.string.empty_string)
            textView.text = result
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

            // Update viewModel
            viewModel.headerText.value = bottomSheetHeader.text.toString()
            viewModel.currentText.value = textView.text.toString()
        }
    }
}
