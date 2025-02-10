package com.example.healthmonitoringapp.adapter;

import android.annotation.SuppressLint;
import android.graphics.Color;
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
    private OnDeviceClickListener connectListener;
    private OnDeviceClickListener unpairListener;
    private Device connectedDevice; // Track connected device

    // ✅ Constructor accepting both connect and unpair listeners
    public DevicesAdapter(List<Device> devices, OnDeviceClickListener connectListener, OnDeviceClickListener unpairListener) {
        this.deviceList = (devices != null) ? devices : new ArrayList<>();
        this.connectListener = connectListener;
        this.unpairListener = unpairListener;
    }

    // ✅ ViewHolder Class
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, addressTextView, signalStrengthTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.deviceName);
            addressTextView = itemView.findViewById(R.id.deviceAddress);
            signalStrengthTextView = itemView.findViewById(R.id.deviceSignalStrength);
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
        Device device = deviceList.get(position);
        boolean isConnected = (connectedDevice != null && connectedDevice.getAddress().equals(device.getAddress()));

        holder.nameTextView.setText(device.getName() != null ? device.getName() : "Unknown Device");
        holder.addressTextView.setText(device.getAddress() != null ? device.getAddress() : "No Address");
        holder.signalStrengthTextView.setText("RSSI: " + device.getRssi() + " dBm");

        // ✅ Highlight connected device
        if (isConnected) {
            holder.nameTextView.setText(device.getName() + " (Connected)");
            holder.nameTextView.setTextColor(Color.GREEN);
        } else {
            holder.nameTextView.setTextColor(Color.BLACK);
        }

        // ✅ Handle device click for connection
        holder.itemView.setOnClickListener(v -> {
            if (connectListener != null) {
                connectListener.onDeviceClick(device);
            }
        });

        // ✅ Handle long press for unpairing
        holder.itemView.setOnLongClickListener(v -> {
            if (unpairListener != null) {
                unpairListener.onDeviceClick(device);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    // ✅ Update list dynamically
    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<Device> newDevices) {
        if (newDevices == null) return;
        deviceList.clear();
        deviceList.addAll(newDevices);
        notifyDataSetChanged();
    }

    // ✅ Highlight connected device
    public void setConnectedDevice(Device device) {
        this.connectedDevice = device;
        notifyDataSetChanged();
    }

    // ✅ Remove device from the list (for swipe-to-delete)
    public void removeDevice(int position) {
        if (position >= 0 && position < deviceList.size()) {
            deviceList.remove(position);
            notifyItemRemoved(position);
        }
    }

    // ✅ Interface for handling device actions
    public interface OnDeviceClickListener {
        void onDeviceClick(Device device);
    }
}
