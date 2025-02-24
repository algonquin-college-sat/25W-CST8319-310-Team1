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

    fun setBoundingBoxes(boxes: List<RectF>) {
        boundingBoxes.clear()
        boundingBoxes.addAll(boxes)
        invalidate()  // Redraw the view

}
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val paint = Paint()
        paint.color = Color.RED
        paint.strokeWidth = 4f
        paint.style = Paint.Style.STROKE

        for (box in boundingBoxes) {
            canvas.drawRect(box, paint)
        }
    }

}
