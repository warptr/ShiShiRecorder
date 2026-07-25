package com.yepgoryo.CaptureCap

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.util.AttributeSet

class OpacitySeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ColorGradientSeekBar(context, attrs) {

    init {
        progress = 100
    }

    private var colorChosen: Int = Color.DKGRAY

    fun setColorChosen(newColor: Int) {
        colorChosen = newColor
        invalidate()
    }

    fun setValueChosen(newValue: Float) {
        val hsv = FloatArray(3)
        Color.colorToHSV(colorChosen, hsv)
        hsv[2] = newValue
        colorChosen = Color.HSVToColor(hsv)
        invalidate()
    }

    override fun createGradientShader(rect: Rect): Shader {
        return LinearGradient(
            0f, 0f, width.toFloat(), 0f,
            intArrayOf(Color.TRANSPARENT, colorChosen),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    fun getOpacityValue(): Float {
        return progress / max.toFloat()
    }

    fun drawCheckers(
        canvas: Canvas,
        squareSize: Int,
        color1: Int = Color.LTGRAY,
        color2: Int = Color.DKGRAY,
        startX: Float = 0f,
        startY: Float = 0f
    ) {
        val paint = Paint()
        paint.style = Paint.Style.FILL

        val clipBounds = canvas.clipBounds

        val startCol = (clipBounds.left / squareSize) - 1
        val endCol = (clipBounds.right / squareSize) + 1
        val startRow = (clipBounds.top / squareSize) - 1
        val endRow = (clipBounds.bottom / squareSize) + 1

        for (row in startRow..endRow) {
            for (col in startCol..endCol) {
                val currentColor = if ((row + col) % 2 == 0) color1 else color2

                paint.color = currentColor

                val left = (col * squareSize) + startX
                val top = (row * squareSize) + startY
                val right = left + squareSize
                val bottom = top + squareSize

                canvas.drawRect(left, top, right, bottom, paint)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        drawCheckers(
            canvas = canvas,
            squareSize = 50,
        )

        super.onDraw(canvas)
    }
}