package com.example.touchlessui

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.touchlessui.R

enum class GameState { READY, RUNNING, GAME_OVER }

class GameView(context: Context, attrs: AttributeSet) : SurfaceView(context, attrs),
    SurfaceHolder.Callback {

  private var state = GameState.READY
  private var gameThread: GameThread? = null

  // Game objects
  private val bird = BirdBitmap(context, BirdBitmap.BirdColor.YELLOW)
  private val pipes = mutableListOf<Pipe>()
  private var score = 0
  private val paint = Paint()
  private val scorePaint = Paint().apply {
    color = Color.WHITE
    typeface = Typeface.DEFAULT_BOLD
    textAlign = Paint.Align.CENTER
    textSize = resources.getDimension(R.dimen.score_text_size)
  }

  private var spawnTicker = 0
  // Spawn pipes a little sooner to keep the game engaging
  private val spawnInterval = 70  // frames between new pipes (~1s at 60fps)

  var isGameOver = false
  var isPaused = false
  var onGameOver: (() -> Unit)? = null

  // How “zoomed in” the world is. 1f = normal, >1f zooms in, <1f zooms out.
  // Previous value was 1.5f which cropped a large portion of the scene.
  // Using 1f shows the entire background and ground.
  private val zoom = 1f

  // Background and ground bitmaps
  private val bgBitmap = BitmapFactory.decodeResource(resources, R.drawable.background_day)
  private val groundBitmap = BitmapFactory.decodeResource(resources, R.drawable.base)
  private var groundOffset = 0f
  private val groundSpeed = 4f // Adjust as needed for your game speed

  init {
    holder.addCallback(this)
  }

  override fun surfaceCreated(holder: SurfaceHolder) {
    // Start (or restart) your game loop thread here:
    gameThread = GameThread(holder, this).also {
      it.running = true
      it.start()
    }
  }

  fun startGame() {
    // Called when user clicks “Ready” or blinks on READY
    state = GameState.RUNNING
    pipes.clear()
    // Pre-load the ticker so the first pipe spawns right away
    spawnTicker = spawnInterval
    score = 0
    bird.reset(width, height)
    isGameOver = false
    isPaused = false
  }

  fun gameOver() {
    state = GameState.GAME_OVER
    isGameOver = true
    post { onGameOver?.invoke() }
  }

  fun pauseGame() {
    if (state == GameState.RUNNING) {
      isPaused = true
    }
  }

  fun resumeGame() {
    if (state == GameState.RUNNING) {
      isPaused = false
    }
  }

  fun flap() {
    if (state == GameState.RUNNING) bird.flap() // Remove zoom argument
  }

  fun update() {
    when (state) {
      GameState.RUNNING -> {
        if (!isPaused) {
          bird.update() // Remove zoom argument
          updatePipes()
          checkCollisions()
        }
      }
      else -> { /* no physics in READY or GAME_OVER */ }
    }
  }

  override fun surfaceChanged(
    holder: SurfaceHolder,
    format: Int,
    width: Int,
    height: Int
  ) {
    // You can handle size changes here if needed (e.g. reposition elements)
  }

  override fun surfaceDestroyed(holder: SurfaceHolder) {
    // Stop your game loop thread cleanly:
    gameThread?.running = false
    // (Optionally join the thread if you want to wait for it to finish)
    gameThread = null
  }

  // In render(), update groundOffset using groundSpeed / zoom for consistent visual speed
  fun render(canvas: Canvas) {
    // 1) Zoomed world
    canvas.save()
    // Scale around the center so the scene remains fully visible
    canvas.scale(zoom, zoom, width / 2f, height / 2f)

    // — Draw background
    canvas.drawBitmap(bgBitmap, null, android.graphics.Rect(0, 0, width, height), null)

    // — Draw scrolling ground
    groundOffset = (groundOffset - groundSpeed / zoom) % groundBitmap.width
    val yGround = height - groundBitmap.height
    canvas.drawBitmap(groundBitmap, groundOffset, yGround.toFloat(), null)
    canvas.drawBitmap(groundBitmap, groundOffset + groundBitmap.width, yGround.toFloat(), null)

    // — Draw pipes, bird, score when running or game over
    when (state) {
      GameState.RUNNING, GameState.GAME_OVER -> {
        pipes.forEach { it.draw(canvas) }
        bird.draw(canvas)
        drawScore(canvas)
      }
      else -> { /* READY state: nothing */ }
    }

    canvas.restore()

    // 2) Un‑zoomed overlays
    if (state == GameState.GAME_OVER) {
      val goBmp = BitmapFactory.decodeResource(resources, R.drawable.gameover)
      // true physical center:
      val cx = (width  - goBmp.width ) / 2f
      val cy = (height - goBmp.height) / 2f
      canvas.drawBitmap(goBmp, cx, cy, null)
    }
  }

  private fun drawScore(canvas: Canvas) {
    // After scale(), (width/2f, yPos) is effectively (width*zoom/2f, yPos*zoom)
    // So to center it on the unscaled screen, halve width and then scale:
    val xPos = (width / 2f) / zoom
    val yPos = (resources.getDimension(R.dimen.score_text_size) + 16f) / zoom
    canvas.drawText("$score", xPos, yPos, scorePaint)
  }

  private fun updatePipes() {
    spawnTicker++
    if (spawnTicker >= spawnInterval) {
      val spawnX = (width / zoom) + 100  // start just off-screen
      pipes.add(Pipe(context, spawnX.toInt(), height))
      spawnTicker = 0
    }
    pipes.forEach {
      it.update()
      if (!it.isScored && it.scored(bird.x)) {
        score++
        it.isScored = true
      }
    }
    pipes.removeAll { it.isOffScreen() }
  }

  private fun checkCollisions() {
    val birdRect = bird.getBounds()
    // ground
    if (bird.y + bird.getBounds().height() >= height) {
      gameOver()
    }
    // pipes
    for (pipe in pipes) {
      if (pipe.collidesWith(birdRect)) {
        gameOver()
        break
      }
    }
  }
}
