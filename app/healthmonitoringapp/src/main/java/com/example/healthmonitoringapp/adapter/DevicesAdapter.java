package com.example.healthmonitoringapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.healthmonitoringapp.Device;
import com.example.healthmonitoringapp.R;

import java.util.List;

public class DevicesAdapter extends ArrayAdapter<Device> {

    public DevicesAdapter(Context context, List<Device> devices) {
        super(context, 0, devices);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_device, parent, false);
        }

        Device device = getItem(position);

        TextView nameTextView = convertView.findViewById(R.id.deviceName);
        TextView addressTextView = convertView.findViewById(R.id.deviceAddress);

        nameTextView.setText(device.getName());
        addressTextView.setText(device.getAddress());

        return convertView;
    }
}
