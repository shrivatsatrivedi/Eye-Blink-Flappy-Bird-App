package com.example.touchlessui

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*

class FaceAnalyzer(
    private val onBlink: () -> Unit,
    private val onNod: () -> Unit
) : ImageAnalysis.Analyzer {
    // Blink detection state
    private var lastState = BlinkState.INIT
    private var lastBlinkTime = 0L
    private val OPEN_THRESHOLD = 0.8f
    private val CLOSE_THRESHOLD = 0.4f
    private val BLINK_COOLDOWN_MS = 200L

    private enum class BlinkState { INIT, OPEN, CLOSED }

    private val detector: FaceDetector

    init {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .enableTracking()
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)  // for eyes open prob
            .build()
        detector = FaceDetection.getClient(options)
    }

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    Log.d("FaceAnalyzer", "No faces detected")
                } else {
                    val face = faces[0]
                    val left  = face.leftEyeOpenProbability ?: -1f
                    val right = face.rightEyeOpenProbability ?: -1f
                    Log.d("FaceAnalyzer", "Eye probs → left: $left  right: $right")
                    handleBlink(face)
                    handleNod(face)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FaceAnalyzer", "Detection failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private var blinkState = 1 // 1 = ready to detect blink, 0 = just blinked
    private fun handleBlink(face: Face) {
        val left = face.leftEyeOpenProbability ?: -1f
        val right = face.rightEyeOpenProbability ?: -1f
        val closedEither = left < 0.4 || right < 0.4
        Log.d("FaceAnalyzer", "handleBlink: left=$left right=$right closedEither=$closedEither blinkState=$blinkState")
        if (closedEither && blinkState == 1) {
            Log.d("FaceAnalyzer", "Blink detected! Calling onBlink()")
            onBlink()
            blinkState = 0
        } else if (!closedEither) {
            blinkState = 1 // Reset when both eyes are open
        }
    }

    private fun handleNod(face: Face) {
        val pitch = face.headEulerAngleX
        if (pitch > 15) {
            onNod()
        }
    }
}
