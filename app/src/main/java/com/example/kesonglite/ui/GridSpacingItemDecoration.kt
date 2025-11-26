package com.example.kesonglite.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

/**
 * 为 StaggeredGridLayoutManager 实现 ItemDecoration，用于设置间距。
 */
class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val layoutParams = view.layoutParams as StaggeredGridLayoutManager.LayoutParams
        val position = parent.getChildAdapterPosition(view) // item position
        val spanIndex = layoutParams.spanIndex // item span index (0 or 1 for 2 columns)

        if (includeEdge) {
            outRect.left = spacing - spanIndex * spacing / spanCount // spacing - column * ((1f / spanCount) * spacing)
            outRect.right = (spanIndex + 1) * spacing / spanCount // (column + 1) * ((1f / spanCount) * spacing)

            if (position < spanCount) { // top edge
                outRect.top = spacing
            }
            outRect.bottom = spacing // item bottom
        } else {
            outRect.left = spanIndex * spacing / spanCount // column * ((1f / spanCount) * spacing)
            outRect.right = spacing - (spanIndex + 1) * spacing / spanCount // spacing - (column + 1) * ((1f / spanCount) * spacing)
            if (position >= spanCount) {
                outRect.top = spacing // item top
            }
        }
    }
}
