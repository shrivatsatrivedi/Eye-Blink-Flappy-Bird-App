package com.example.touchlessui

import android.graphics.Canvas
import android.view.SurfaceHolder

class GameThread(
    private val holder: SurfaceHolder,
    private val gameView: GameView
) : Thread() {
    var running = false

    override fun run() {
        while (running) {
            val canvas: Canvas? = holder.lockCanvas()
            canvas?.let {
                if (!gameView.isPaused) {
                    gameView.update()
                }
                gameView.render(it)
                holder.unlockCanvasAndPost(it)
            }
            sleep(16)  // ~60fps
        }
    }
}

