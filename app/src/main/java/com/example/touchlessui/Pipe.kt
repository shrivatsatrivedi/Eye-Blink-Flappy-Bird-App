package com.example.touchlessui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.random.Random

class Pipe(context: Context, private val screenWidth: Int, private val screenHeight: Int) {
    private val topBmp: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.pipe_green)
    private val bottomBmp: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.pipe_green)

    // Vertical gap size between top & bottom pipe
    private val gap = 400  // adjust for difficulty
    private val speed = 12f

    // X position starts off-screen to the right
    var x = screenWidth.toFloat()
    var isScored = false
    // Randomize center of gap between some margins
    private val centerY = Random.nextInt(gap, screenHeight - gap)

    fun update() {
        x -= speed
    }

    fun draw(canvas: Canvas) {
        // Top pipe: rotated upside down
        val topRect = RectF(
            x,
            0f,
            x + topBmp.width,
            centerY - gap / 2f
        )
        canvas.save()
        canvas.rotate(180f, x + topBmp.width / 2, (centerY - gap / 2f) / 2)
        canvas.drawBitmap(topBmp, null, topRect, null)
        canvas.restore()

        // Bottom pipe
        val bottomRect = RectF(
            x,
            centerY + gap / 2f,
            x + bottomBmp.width,
            screenHeight.toFloat()
        )
        canvas.drawBitmap(bottomBmp, null, bottomRect, null)
    }

    /** Returns true when this pipe is fully off-screen to the left **/
    fun isOffScreen(): Boolean = x + topBmp.width < 0

    /** Check if birdRect intersects with either pipe **/
    fun collidesWith(birdRect: RectF): Boolean {
        val topRect = RectF(x, 0f, x + topBmp.width, centerY - gap / 2f)
        val bottomRect = RectF(x, centerY + gap / 2f, x + bottomBmp.width, screenHeight.toFloat())
        return RectF.intersects(topRect, birdRect) || RectF.intersects(bottomRect, birdRect)
    }

    /** True if bird has just passed the pipe (for scoring) **/
    fun scored(birdX: Float): Boolean = (x + topBmp.width) < birdX
}

