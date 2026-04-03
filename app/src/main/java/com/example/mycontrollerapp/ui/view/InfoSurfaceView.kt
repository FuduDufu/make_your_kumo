package com.example.mycontrollerapp.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class InfoSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    @Volatile private var voltage: Double? = null
    @Volatile private var current: Double? = null
    @Volatile private var bps: Int? = null
    @Volatile private var ip: String? = null
    @Volatile private var touch = BooleanArray(6) { false } // L1 L2 L3 R1 R2 R3

    fun setTelemetry(voltage: Double?, current: Double?, bps: Int?, ip: String?) {
        this.voltage = voltage
        this.current = current
        this.bps = bps
        this.ip = ip
        postInvalidateOnAnimation()
    }

    fun setTouchStates(states6: BooleanArray) {
        if (states6.size == 6) touch = states6.copyOf()
        postInvalidateOnAnimation()
    }

    // UI дээр харуулах текст/мэдээлэл (engine-ээс update хийнэ)


    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        textSize = 32f
    }

    private val touchOffPaint = Paint().apply { color = Color.BLACK }
    private val touchOnPaint  = Paint().apply { color = Color.RED }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.rgb(60, 60, 60))

        // 6 квадрат байрлал (дэлгэцийн 6 тал)
        val w = width.toFloat()
        val h = height.toFloat()
        val pad = 18f
        val box = (w * 0.12f).coerceAtLeast(70f)

        val topY = 120f
        val midY = h * 0.45f
        val botY = h * 0.78f

        val leftX = pad
        val rightX = w - pad - box

        val rects = arrayOf(
            android.graphics.RectF(leftX,  topY, leftX + box,  topY + box), // L1
            android.graphics.RectF(leftX,  midY, leftX + box,  midY + box), // L2
            android.graphics.RectF(leftX,  botY, leftX + box,  botY + box), // L3
            android.graphics.RectF(rightX, topY, rightX + box, topY + box), // R1
            android.graphics.RectF(rightX, midY, rightX + box, midY + box), // R2
            android.graphics.RectF(rightX, botY, rightX + box, botY + box)  // R3
        )

        for (i in 0 until 6) {
            val p = if (touch.getOrNull(i) == true) touchOnPaint else touchOffPaint
            canvas.drawRect(rects[i], p)
        }

        val vText = voltage?.let { String.format("V: %.2f", it) } ?: "V: ---"
        val iText = current?.let { String.format("I: %.2f", it) } ?: "I: ---"
        val bText = bps?.let { "BPS: $it" } ?: "BPS: -"
        val ipText = ip?.let { "IP:  $it" } ?: "IP:  ---"

        canvas.drawText(vText, 140f, 110f, textPaint)
        canvas.drawText(iText, 140f, 150f, textPaint)
        canvas.drawText(bText, 140f, 200f, textPaint)
        canvas.drawText(ipText, 140f, 240f, textPaint)
    }
}