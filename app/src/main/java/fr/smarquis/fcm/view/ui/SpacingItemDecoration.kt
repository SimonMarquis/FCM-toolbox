package fr.smarquis.fcm.view.ui

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.annotation.Px
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import kotlin.math.max

class SpacingItemDecoration(@param:Px private val horizontal: Int, @param:Px private val vertical: Int) : ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val dy = vertical / 2
        val dx = when (val viewWidth = view.layoutParams.width) {
            MATCH_PARENT, WRAP_CONTENT -> horizontal
            else -> max(horizontal, (parent.width - viewWidth) / 2)
        }
        outRect.set(dx, dy, dx, dy)
    }

}