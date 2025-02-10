package com.example.healthmonitoringapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthmonitoringapp.R;
import com.example.healthmonitoringapp.model.Device;

import java.util.ArrayList;
import java.util.List;

public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder> {
    private List<Device> deviceList;
    private OnDeviceClickListener listener;

    // Constructor with null safety
    public DevicesAdapter(List<Device> devices, OnDeviceClickListener listener) {
        this.deviceList = (devices != null) ? devices : new ArrayList<>();
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, addressTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.deviceName);
            addressTextView = itemView.findViewById(R.id.deviceAddress);
        }

        // Bind data to views
        public void bind(Device device, OnDeviceClickListener listener) {
            nameTextView.setText(device.getName() != null ? device.getName() : "Unknown Device");
            addressTextView.setText(device.getAddress() != null ? device.getAddress() : "No Address");

            // Handle item click event
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeviceClick(device);
                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(deviceList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    // ✅ Optimized method to update the device list dynamically
    public void updateList(List<Device> newDevices) {
        if (newDevices == null) return; // Prevent null crash
        deviceList.clear();
        deviceList.addAll(newDevices);
        notifyItemRangeChanged(0, deviceList.size()); // More efficient than notifyDataSetChanged()
    }

    // Interface for handling click events
    public interface OnDeviceClickListener {
        void onDeviceClick(Device device);
    }
}
