package com.example.healthmonitoringapp.adapter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthmonitoringapp.R;

public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {
    private final DevicesAdapter adapter;
    private final Drawable deleteIcon;
    private final ColorDrawable background;

    public SwipeToDeleteCallback(Context context, DevicesAdapter adapter) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.adapter = adapter;
        this.deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete);  // Ensure this icon exists
        this.background = new ColorDrawable(Color.RED);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;  // No need for drag-and-drop
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getBindingAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
            adapter.removeItem(position);
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            View itemView = viewHolder.itemView;
            int itemHeight = itemView.getHeight();
            int itemTop = itemView.getTop();
            int itemBottom = itemView.getBottom();
            int itemLeft = itemView.getLeft();
            int itemRight = itemView.getRight();
            int iconMargin = (itemHeight - deleteIcon.getIntrinsicHeight()) / 2;

            if (dX > 0) {  // Swiping right
                background.setBounds(itemLeft, itemTop, (int) dX, itemBottom);
                deleteIcon.setBounds(itemLeft + iconMargin, itemTop + iconMargin,
                        itemLeft + iconMargin + deleteIcon.getIntrinsicWidth(), itemBottom - iconMargin);
            } else if (dX < 0) {  // Swiping left
                background.setBounds(itemRight + (int) dX, itemTop, itemRight, itemBottom);
                deleteIcon.setBounds(itemRight - iconMargin - deleteIcon.getIntrinsicWidth(), itemTop + iconMargin,
                        itemRight - iconMargin, itemBottom - iconMargin);
            } else {
                background.setBounds(0, 0, 0, 0);
            }

            background.draw(canvas);
            deleteIcon.draw(canvas);
        }

        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
