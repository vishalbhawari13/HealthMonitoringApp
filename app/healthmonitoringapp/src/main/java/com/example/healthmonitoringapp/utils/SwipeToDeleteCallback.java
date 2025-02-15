package com.example.healthmonitoringapp.utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.example.healthmonitoringapp.adapter.DevicesAdapter;

public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {
    private DevicesAdapter adapter;

    public SwipeToDeleteCallback(DevicesAdapter adapter) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.adapter = adapter;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false; // No movement support, only swipe
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        adapter.removeDevice(position); // ✅ Remove device from list
    }
}