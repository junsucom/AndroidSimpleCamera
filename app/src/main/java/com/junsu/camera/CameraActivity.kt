package com.junsu.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.junsu.camera.databinding.ActivityCameraBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null
    private lateinit var binding: ActivityCameraBinding
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService


    private val requestMultiplePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        Log.d(TAG, "RequestMultiplePermissions cb")
        if (it.values.contains(false)) {
            val appDetail = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            appDetail.addCategory(Intent.CATEGORY_DEFAULT)
            appDetail.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(appDetail)
        } else {
            startCamera()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the listener for take photo button
        binding.cameraCaptureButton.setOnClickListener { takePhoto() }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Request camera permissions
        if (PermissionUtils.requestPermission(this, CAMERA_PERMISSIONS_REQUEST, Manifest.permission.CAMERA)) {
            startCamera()
        } else {
            requestMultiplePermissions.launch(arrayOf(Manifest.permission.CAMERA))
//            ActivityCompat.requestPermissions(
//                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)?.let { fileUri ->
            val outputStream = contentResolver.openOutputStream(fileUri)!!
            val outputOptions = ImageCapture.OutputFileOptions.Builder(outputStream).build()
            imageCapture.takePicture(
                outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val msg = "!!!! Photo capture succeeded"
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        Log.d(TAG, msg)
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                })
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(
            {
                // Used to bind the lifecycle of cameras to the lifecycle owner
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .setTargetRotation(binding.viewFinder.display.rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                // Preview
                val previewBuilder = Preview.Builder()
                val preview = previewBuilder.build()
                preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)

                imageCapture = ImageCapture.Builder()
                    .build()

                val cameraSelector = if(intent.hasExtra(BACK_CAMERA)) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }
                // Select back camera as a default


                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    val camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture, imageAnalysis,
                    )

                    camera.cameraControl.cancelFocusAndMetering()

                    val enableTorchLF = camera.cameraControl.enableTorch(false)
//                    enableTorchLF.addListener({
//                        enableTorchLF.get()
//                    }, cameraExecutor)

                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                }

            },
            ContextCompat.getMainExecutor(this),
        )
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val CAMERA_PERMISSIONS_REQUEST = 2
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val BACK_CAMERA = "back"
    }
}