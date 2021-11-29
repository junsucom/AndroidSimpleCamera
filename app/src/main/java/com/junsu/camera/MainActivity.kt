package com.junsu.camera

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.junsu.camera.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {
    companion object {
        var photoUri: Uri? = null
    }
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.fab.setOnClickListener { view ->
            startCamera()
        }
    }

    private fun getCameraFile(): File {
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("temp", "-", dir)
    }

    private fun startCamera() {
        val intent = Intent(this, CameraActivity::class.java).apply {
            photoUri = FileProvider.getUriForFile(
                this@MainActivity,
                "${applicationContext.packageName}.fileprovider",
                getCameraFile()
            )
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        }
        cameraRequestResult.launch(intent)
    }

    private val cameraRequestResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                photoUri = if (it.data != null) {
                    it.data!!.data
                } else {
                    photoUri
                }
                photoUri?.let { uri ->
                    process(uri)
                } ?: toast("사진을 찾을 수 없습니다.")

            }
        }

    private val scopeExceptionHandler = CoroutineExceptionHandler { _, e ->
        e.message?.let { toast(it) }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun process(uri: Uri) {
        lifecycleScope.launch(scopeExceptionHandler) {
            Glide.with(this@MainActivity)
                .load(uri)
                .into(binding.main.mainImage)
        }
    }

}