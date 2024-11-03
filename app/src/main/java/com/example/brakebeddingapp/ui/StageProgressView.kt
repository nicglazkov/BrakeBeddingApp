package com.example.brakebeddingapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class StageProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val margin = 8f // dp
    private val marginPx = margin * resources.displayMetrics.density
    private val cornerRadius = 4f * resources.displayMetrics.density

    private var stages: List<BeddingStage> = emptyList()
    private var currentStage = 0
    private var currentCycle = 0

    private val completedColor = ContextCompat.getColor(context, android.R.color.holo_green_dark)
    private val currentColor = ContextCompat.getColor(context, android.R.color.holo_blue_light)
    private val futureColor = ContextCompat.getColor(context, android.R.color.darker_gray)
    private val textColor = ContextCompat.getColor(context, android.R.color.white)

    fun updateProgress(stages: List<BeddingStage>, currentStage: Int, currentCycle: Int) {
        this.stages = stages
        this.currentStage = currentStage
        this.currentCycle = currentCycle
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (stages.isEmpty()) return

        val stageWidth = (width - (stages.size + 1) * marginPx) / stages.size
        val stageHeight = height - 2 * marginPx

        stages.forEachIndexed { stageIndex, stage ->
            val x = marginPx + (stageWidth + marginPx) * stageIndex
            val numCycles = stage.numberOfStops
            val cycleHeight = stageHeight / numCycles

            for (cycleIndex in 0 until numCycles) {
                val y = marginPx + cycleHeight * cycleIndex

                // Set color based on progress
                paint.color = when {
                    stageIndex < currentStage -> completedColor
                    stageIndex == currentStage && cycleIndex < currentCycle -> completedColor
                    stageIndex == currentStage && cycleIndex == currentCycle -> currentColor
                    else -> futureColor
                }

                // Draw cycle rectangle
                rect.set(x, y, x + stageWidth, y + cycleHeight - marginPx)
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

                // Draw cycle number
                if (cycleHeight >= 24f * resources.displayMetrics.density) { // Only draw text if there's enough space
                    paint.color = textColor
                    paint.textSize = cycleHeight * 0.4f
                    paint.textAlign = Paint.Align.CENTER
                    val textX = x + stageWidth / 2
                    val textY = y + cycleHeight / 2 + paint.textSize / 3
                    canvas.drawText("${cycleIndex + 1}", textX, textY, paint)
                }
            }

            // Draw stage number above the cycles
            paint.color = textColor
            paint.textSize = marginPx * 1.5f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Stage ${stageIndex + 1}", x, marginPx - 2, paint)
        }
    }
}