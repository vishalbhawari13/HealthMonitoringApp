package com.example.healthmonitoringapp.utils;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import com.example.healthmonitoringapp.model.Device;

import java.util.ArrayList;
import java.util.List;

public class BluetoothRepository {

    private final BluetoothAdapter bluetoothAdapter;

    public BluetoothRepository() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public List<Device> getPairedDevices(Context context) {
        List<Device> devices = new ArrayList<>();

        // Check if Bluetooth is supported
        if (bluetoothAdapter == null) {
            return devices; // Return empty list if Bluetooth is not available
        }

        // Check for necessary permissions on Android 12+ before accessing bonded devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return devices; // Return empty list if permission is not granted
        }

        // Fetch bonded devices
        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            String deviceName = device.getName();
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = "Unknown Device"; // Handle unnamed devices
            }
            devices.add(new Device(deviceName, device.getAddress()));
        }
        return devices;
    }
}
