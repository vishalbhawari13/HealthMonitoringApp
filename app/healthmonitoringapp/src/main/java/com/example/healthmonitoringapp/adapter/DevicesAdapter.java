package com.example.healthmonitoringapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthmonitoringapp.R;
import com.example.healthmonitoringapp.model.Device;

import java.util.List;

public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder> {
    private List<Device> deviceList;

    public DevicesAdapter(List<Device> devices) {
        this.deviceList = devices;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, addressTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.deviceName);
            addressTextView = itemView.findViewById(R.id.deviceAddress);
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
        holder.nameTextView.setText(device.getName() != null ? device.getName() : "Unknown Device");
        holder.addressTextView.setText(device.getAddress() != null ? device.getAddress() : "No Address");
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    // ✅ Method to update the device list dynamically
    public void updateList(List<Device> newDevices) {
        deviceList.clear();
        deviceList.addAll(newDevices);
        notifyDataSetChanged();
    }
}
