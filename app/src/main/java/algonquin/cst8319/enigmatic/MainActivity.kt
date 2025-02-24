package algonquin.cst8319.enigmatic

import algonquin.cst8319.enigmatic.databinding.ActivityMainBinding
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@ExperimentalGetImage class MainActivity : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var binding : ActivityMainBinding

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
            val cameraProvider: ProcessCameraProvider = processCameraProvider.get()

            // Set up the Preview use case
            val preview = Preview.Builder().build().also {
                val previewView = binding.previewView
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            try {
                // Bind the camera to the lifecycle
                cameraProvider.unbindAll()

                // instantiate the ImageAnalyzer and bind it to the cameraProvider
                val imageAnalyzer = ImageAnalyzer(binding)
                val imageAnalysis = imageAnalyzer.createImageAnalysis(cameraExecutor)

                cameraProvider.bindToLifecycle(
                    this as LifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis
                )
            } catch (e: Exception) {
                Log.d("ERROR", e.message.toString())

            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}