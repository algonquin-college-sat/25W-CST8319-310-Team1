package algonquin.cst8319.enigmatic

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class BoundingBoxView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val boundingBoxes = mutableListOf<RectF>()
    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    fun setBoundingBoxes(boxes: List<RectF>) {
        boundingBoxes.clear()
        boundingBoxes.addAll(boxes)
        invalidate()  // Trigger redraw
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        boundingBoxes.forEach { box ->
            canvas.drawRect(box, paint)
        }
    }
}
