package com.yepgoryo.CaptureCap

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatSeekBar

open class ColorGradientSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.progressBarStyle
) : AppCompatSeekBar(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        isFocusable = false

        thumbPaint.style = Paint.Style.FILL
        thumbPaint.color = Color.WHITE
        thumbPaint.strokeWidth = 2f
        thumbPaint.style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val ratio = progress / max.toFloat()

        if (ratio in 0.0f..0.25f) {
            thumbPaint.style = Paint.Style.STROKE
            thumbPaint.color = Color.WHITE
        } else if (ratio in 0.25f..0.5f) {
            thumbPaint.style = Paint.Style.STROKE
            thumbPaint.color = Color.LTGRAY
        } else if (ratio in 0.5f..0.75f) {
            thumbPaint.style = Paint.Style.STROKE
            thumbPaint.color = Color.DKGRAY
        } else {
            thumbPaint.style = Paint.Style.STROKE
            thumbPaint.color = Color.BLACK
        }

        val rect = Rect(0, 0, width, height)
        val shader = createGradientShader(rect)
        paint.shader = shader

        canvas.drawRect(rect, paint)

        val maxProgress = max.toFloat()
        val progressRatio = if (maxProgress > 0) this.progress / maxProgress else 0f
        val thumbX = (width * progressRatio).coerceIn(0f, width.toFloat())

        val centerY = height / 2f
        val thumbRadius = (height / 2f) * 0.9f

        canvas.drawCircle(thumbX, centerY, thumbRadius, thumbPaint)
        paint.shader = null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val x = event.x.coerceIn(0f, width.toFloat())

                val newProgressRatio = x / width
                val newProgress = (newProgressRatio * 100).toInt()

                progress = newProgress
                invalidate()
            }
        }
        return super.onTouchEvent(event)
    }

    protected open fun createGradientShader(rect: Rect): Shader {
        return LinearGradient(
            0f,
            0f,
            width.toFloat(),
            0f,
            Color.WHITE,
            Color.BLACK,
            Shader.TileMode.CLAMP
        )
    }
}