package com.laurentdarl.churchadministration.data.utility

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class VerticalSpacingDecorator: RecyclerView.ItemDecoration() {
    private var verticalSpaceHeight = 0

    fun VerticalSpacingDecorator(verticalSpaceHeight: Int) {
        this.verticalSpaceHeight = verticalSpaceHeight
    }

    fun getItemOffset(outRect: Rect, view: View?, parent: RecyclerView, state: RecyclerView.State?) {
        outRect.bottom = verticalSpaceHeight
        if (parent.getChildAdapterPosition(view!!) !== parent.adapter!!.itemCount - 1) {
            outRect.bottom = verticalSpaceHeight
        }
    }
}