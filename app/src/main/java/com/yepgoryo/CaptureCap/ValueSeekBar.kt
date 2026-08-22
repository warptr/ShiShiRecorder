/*
 * This code has been co-authored by an AI.
 * Model name: Qwen 3 Coder Next
 */

package com.yepgoryo.CaptureCap

import android.content.Context

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Rect
import android.graphics.Shader
import android.util.AttributeSet

class ValueSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ColorGradientSeekBar(context, attrs) {

    init {
        progress = 100
    }
    private var colorChosen: Int = Color.DKGRAY
    private var colorGradientChosen: Int = Color.DKGRAY

    fun setColorChosen(newColor: Int) {
        colorChosen = newColor

        val hsv = FloatArray(3)
        Color.colorToHSV(colorChosen, hsv)
        progress = (hsv[2] * max.toFloat()).toInt()
        setColorGradient(newColor)

        invalidate()
    }

    fun setColorGradient(newColor: Int) {
        colorGradientChosen = getColorForGradient(newColor)
        invalidate()
    }

    fun getColorForGradient(newColor: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(newColor, hsv)
        hsv[2] = 1.0f
        return Color.HSVToColor(hsv)
    }

    override fun createGradientShader(rect: Rect): Shader {
        return LinearGradient(
            0f, 0f, width.toFloat(), 0f,
            intArrayOf(Color.BLACK, colorGradientChosen),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    fun getValueValue(): Float {
        return progress / max.toFloat()
    }

    fun getColorWithValue(): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(colorGradientChosen, hsv)
        hsv[2] = getValueValue()
        return Color.HSVToColor(hsv)
    }

}