package com.example.healthmonitoringapp.utils;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import com.example.healthmonitoringapp.model.Device;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothRepository {
    private final BluetoothAdapter bluetoothAdapter;

    public BluetoothRepository(Context context) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = (bluetoothManager != null) ? bluetoothManager.getAdapter() : null;
    }

    public List<Device> getPairedDevices(Context context) {
        List<Device> devices = new ArrayList<>();

        // Check if Bluetooth is supported
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            return devices; // Return empty list if Bluetooth is not available or disabled
        }

        // Check for necessary permissions on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return devices; // Return empty list if permission is not granted
        }

        // Fetch bonded (paired) devices
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : bondedDevices) {
            if (device != null && device.getAddress() != null) {
                String deviceName = (device.getName() != null) ? device.getName() : "Unknown Device";
                devices.add(new Device(deviceName, device.getAddress(), 0)); // ✅ Added default RSSI value

            }
        }
        return devices;
    }

    // ✅ Check if Bluetooth is enabled
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    // ✅ Enable Bluetooth (Requires User Interaction)
    public boolean enableBluetooth(Context context) {
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return false; // Permission not granted, return false
                }
            }
            return bluetoothAdapter.enable(); // ✅ Safe to enable Bluetooth
        }
        return false;
    }


    // ✅ Disable Bluetooth
    public boolean disableBluetooth(Context context) {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return false; // Permission not granted, return false
                }
            }
            return bluetoothAdapter.disable(); // ✅ Safe to disable Bluetooth
        }
        return false;
    }


    // ✅ Check if Bluetooth permissions are granted
    public boolean hasBluetoothPermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Older versions don't require this permission
    }
}
