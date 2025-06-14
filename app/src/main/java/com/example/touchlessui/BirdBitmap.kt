package com.example.touchlessui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF

class BirdBitmap(context: Context, birdColor: BirdColor) {
    enum class BirdColor {
        BLUE, YELLOW, RED
    }

    // Load the three frames for one bird color (pick blue/red/yellow)
    private val frames: List<Bitmap> = when (birdColor) {
        BirdColor.BLUE -> listOf(
            BitmapFactory.decodeResource(context.resources, R.drawable.bluebird_upflap),
            BitmapFactory.decodeResource(context.resources, R.drawable.bluebird_midflap),
            BitmapFactory.decodeResource(context.resources, R.drawable.bluebird_downflap)
        )
        BirdColor.YELLOW -> listOf(
            BitmapFactory.decodeResource(context.resources, R.drawable.yellowbird_upflap),
            BitmapFactory.decodeResource(context.resources, R.drawable.yellowbird_midflap),
            BitmapFactory.decodeResource(context.resources, R.drawable.yellowbird_downflap)
        )
        BirdColor.RED -> listOf(
            BitmapFactory.decodeResource(context.resources, R.drawable.redbird_upflap),
            BitmapFactory.decodeResource(context.resources, R.drawable.redbird_midflap),
            BitmapFactory.decodeResource(context.resources, R.drawable.redbird_downflap)
        )
    }

    // Position & physics
    var x = 0f
    var y = 0f
    private var velocityY = 0f

    // Animation state
    private var frameIndex = 0
    private var frameTicker = 0

    companion object {
        // Tune the physics constants to better match the original game feel
        private const val GRAVITY = 0.4f
        private const val FLAP_STRENGTH = -9f
        private const val FRAME_DELAY = 3  // Faster animation
    }

    // Store the constructor context parameter as a private property
    private val context: Context = context

    init {
        // Position will be set by reset(viewWidth, viewHeight)
    }

    fun reset(viewWidth: Int, viewHeight: Int) {
        velocityY = 0f
        frameIndex = 0
        frameTicker = 0
        // Set y to center of view, x to 1/3 of view width
        x = viewWidth / 3f
        y = viewHeight / 2f
    }

    fun flap() {
        velocityY = FLAP_STRENGTH
    }

    private var lastUpdateTime: Long = 0L

    fun update() {
        val currentTime = System.nanoTime()
        if (lastUpdateTime == 0L) {
            lastUpdateTime = currentTime
            return // Skip the first frame to avoid a huge delta
        }
        val deltaTime = (currentTime - lastUpdateTime) / 1_000_000_000f // seconds
        lastUpdateTime = currentTime

        // Use deltaTime for smooth physics. 60f closely matches the classic timing
        val scale = 60f
        velocityY += GRAVITY * deltaTime * scale
        y += velocityY * deltaTime * scale

        // Cycle animation frames
        frameTicker = (frameTicker + 1) % (FRAME_DELAY * frames.size)
        frameIndex = frameTicker / FRAME_DELAY
    }

    fun draw(canvas: Canvas) {
        // Draw the current frame at (x, y)
        val bmp = frames[frameIndex]
        val dst = RectF(x, y, x + bmp.width, y + bmp.height)
        canvas.drawBitmap(bmp, null, dst, null)
    }

    /** Get bounding rect for collision detection **/
    fun getBounds(): RectF {
        val bmp = frames[frameIndex]
        return RectF(x, y, x + bmp.width, y + bmp.height)
    }
}