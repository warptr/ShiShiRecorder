/*
 * This code has been co-authored by an AI.
 * Model name: Qwen 3 Coder Next
 */

package com.yepgoryo.CaptureCap

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat

class ColorPaletteWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var colorItemClickListener: ((Int, Int) -> Unit)? = null
    private val colors = mutableListOf<Int>()
    private val circleViews = mutableListOf<ImageView>()

    init {
        orientation = HORIZONTAL
        gravity = android.view.Gravity.CENTER
        setWillNotDraw(false)

        val defaultColors = intArrayOf(
            ContextCompat.getColor(context, android.R.color.holo_red_dark),
            ContextCompat.getColor(context, android.R.color.holo_orange_light),
            ContextCompat.getColor(context, android.R.color.holo_green_light),
            ContextCompat.getColor(context, android.R.color.holo_blue_dark),
            ContextCompat.getColor(context, android.R.color.holo_purple)
        )
        setColorList(defaultColors)
    }

    fun setOnColorItemClickListener(listener: (colorIndex: Int, colorValue: Int) -> Unit) {
        this.colorItemClickListener = listener
    }

    fun setColorList(colors: IntArray) {
        removeViews(0, childCount)
        this.colors.clear()
        this.circleViews.clear()

        for (i in colors.indices) {
            addCircle(colors[i], i)
            this.colors.add(colors[i])
        }
    }

    private fun addCircle(color: Int, index: Int) {
        val circle = ImageView(context).apply {
            id = generateViewId()
            setImageResource(R.drawable.circle_background)
            layoutParams = LayoutParams(
                70,
                LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 5
                marginEnd = 5
            }

            setImageTintList(ColorStateList.valueOf(color))

            isClickable = true
            isFocusable = true

            setOnClickListener {
                colorItemClickListener?.invoke(index, color)
            }
        }

        addView(circle)
        circleViews.add(circle)
    }

    fun setColorAt(index: Int, newColor: Int) {
        if (index in 0 until childCount) {
            val circle = getChildAt(index) as ImageView
            circle.setImageTintList(ColorStateList.valueOf(newColor))
            colors[index] = newColor
        }
    }
}