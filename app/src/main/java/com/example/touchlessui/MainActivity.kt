package com.example.touchlessui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.camera.view.PreviewView

class MainActivity : AppCompatActivity() {
    private lateinit var gameView: GameView
    private lateinit var ivReady: ImageView
    private lateinit var ivPause: ImageView
    private lateinit var ivPlay: ImageView
    private lateinit var previewView: PreviewView

    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera() else finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        gameView = findViewById(R.id.gameView)
        ivReady = findViewById(R.id.ivReady)
        ivPause = findViewById(R.id.ivPause)
        ivPlay = findViewById(R.id.ivPlay)
        previewView = findViewById(R.id.previewView)

        // DEBUG: show a toast whenever the overlay is tapped
        ivReady.setOnClickListener {
            Toast.makeText(this, "Tapped READY – starting game…", Toast.LENGTH_SHORT).show()
            it.visibility = View.GONE        // hide the overlay
            ivPause.visibility = View.VISIBLE
            ivPlay.visibility = View.GONE
            gameView.startGame()            // switch to RUNNING
        }

        ivPause.setOnClickListener {
            gameView.pauseGame()
            ivPause.visibility = View.GONE
            ivPlay.visibility = View.VISIBLE
        }

        ivPlay.setOnClickListener {
            gameView.resumeGame()
            ivPlay.visibility = View.GONE
            ivPause.visibility = View.VISIBLE
        }

        gameView.onGameOver = {
            ivPause.visibility = View.GONE
            ivPlay.visibility = View.GONE
            ivReady.visibility = View.VISIBLE
        }

        // Request camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            cameraPermission.launch(Manifest.permission.CAMERA)
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // 1) Preview use‑case, feeding the invisible PreviewView
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            // 2) Analysis use‑case
            val analyzerUseCase = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(ContextCompat.getMainExecutor(this),
                    FaceAnalyzer(
                        onBlink = { runOnUiThread {
                            if (gameView.isGameOver) {
                                ivPause.visibility = View.VISIBLE
                                ivPlay.visibility = View.GONE
                                gameView.startGame()
                            } else {
                                Log.d("MainActivity", "Blink detected → flap()")
                                gameView.flap()
                            }
                        } },
                        onNod   = { runOnUiThread {
                            // Optionally implement pause/resume logic here if needed
                        } }
                    )
                ) }

            // 3) Bind both to lifecycle
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                analyzerUseCase
            )
        }, ContextCompat.getMainExecutor(this))
    }
}