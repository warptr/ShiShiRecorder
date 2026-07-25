package com.yepgoryo.CaptureCap

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.atan2

class ColorPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var paint = Paint()
    private var pickPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var currentHue = 0f
    private var currentSaturation = 1.0f

    private var radius = 0f
    private var innerRadiusScale = 0.8f
    private var centerPoint = PointF()
    private var bitmapSize = 0
    private var bitmapCenter = 0

    init {
        isClickable = true
        isFocusable = true

        currentHue = 30f
        currentSaturation = 1.0f

        post {
            setupColorWheelMatrix()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        radius = (min(w, h) / 2.0).toFloat()
        centerPoint.set(w / 2f, h / 2f)

        setupColorWheelMatrix()
    }

    private fun setupColorWheelMatrix() {
        val size = (getInnerRadius() * 2).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)

        val centerX = radius
        val centerY = radius

        for (y in 0 until size) {
            for (x in 0 until size) {
                val dx = x - centerX
                val dy = y - centerY

                val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                val saturation = if (distance > getInnerRadius()) 1f else distance / getInnerRadius()

                var angle = atan2(dy.toDouble(), dx.toDouble())
                angle = Math.toDegrees(angle) + 180.0

                val hue = angle.toFloat()

                pixels[y * size + x] = Color.HSVToColor(floatArrayOf(hue, saturation, 1.0f))
            }
        }

        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)

        val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.shader = shader

        this.bitmapSize = size
        this.bitmapCenter = centerX.toInt()
    }

    fun getInnerRadius(): Float {
        return radius * innerRadiusScale
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawCircle(centerPoint.x, centerPoint.y, getInnerRadius(), paint)

        val angleRad = Math.toRadians(currentHue.toDouble() - 180)
        val dist = currentSaturation * getInnerRadius()

        val pickX = centerPoint.x + (dist * kotlin.math.cos(angleRad)).toFloat()
        val pickY = centerPoint.y + (dist * kotlin.math.sin(angleRad)).toFloat()

        pickPaint.color = Color.HSVToColor(floatArrayOf(currentHue, currentSaturation, 1.0f))

        if (currentSaturation < 0.3f) pickOutlinePaint.color = Color.BLACK else pickOutlinePaint.color = Color.WHITE

        canvas.drawCircle(pickX, pickY, 20f, pickPaint)
        canvas.drawCircle(pickX, pickY, 20f, pickOutlinePaint)

        val whiteDotPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (currentSaturation < 0.3f) whiteDotPaint.color = Color.BLACK else whiteDotPaint.color = Color.WHITE

        canvas.drawCircle(pickX, pickY, 6f, whiteDotPaint)
    }

    private val pickOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.WHITE
        strokeCap = Paint.Cap.ROUND
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val x = event.x
                val y = event.y

                val dx = centerPoint.x - x
                val dy = centerPoint.y - y

                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
                if (angle < 0) angle += 360.0
                currentHue = angle.toFloat()

                val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                currentSaturation = min(distance / getInnerRadius(), 1.0f)

                onColorChangedListener?.invoke(
                    Color.HSVToColor(floatArrayOf(currentHue, currentSaturation, 1.0f))
                )

                invalidate()
            }
        }
        return true
    }

    fun setColor(color: Int) {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)

        currentHue = hsv[0]
        currentSaturation = hsv[1]

        invalidate()
    }

    fun getCurrentColor(): Int {
        return Color.HSVToColor(floatArrayOf(currentHue, currentSaturation, 1.0f))
    }

    var onColorChangedListener: ((Int) -> Unit)? = null
}