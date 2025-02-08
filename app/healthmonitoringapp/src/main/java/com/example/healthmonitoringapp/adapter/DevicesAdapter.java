package com.example.healthmonitoringapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.healthmonitoringapp.model.Device;
import com.example.healthmonitoringapp.R;

import java.util.List;

public class DevicesAdapter extends ArrayAdapter<Device> {

    public DevicesAdapter(Context context, List<Device> devices) {
        super(context, 0, devices);
    }

    private static class ViewHolder {
        TextView nameTextView;
        TextView addressTextView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_device, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.nameTextView = convertView.findViewById(R.id.deviceName);
            viewHolder.addressTextView = convertView.findViewById(R.id.deviceAddress);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Device device = getItem(position);
        if (device != null) {
            viewHolder.nameTextView.setText(device.getName() != null ? device.getName() : "Unknown Device");
            viewHolder.addressTextView.setText(device.getAddress() != null ? device.getAddress() : "No Address");
        }

        return convertView;
    }
}
