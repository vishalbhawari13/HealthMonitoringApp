package com.example.healthmonitoringapp.utils;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.healthmonitoringapp.model.Device;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothRepository {
    private static final String TAG = "BluetoothRepository";
    private final BluetoothAdapter bluetoothAdapter;
    private final List<Device> discoveredDevices = new ArrayList<>();

    public BluetoothRepository(Context context) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = (bluetoothManager != null) ? bluetoothManager.getAdapter() : null;
    }

    /** ✅ Get Paired Devices */
    public List<Device> getPairedDevices(Context context) {
        List<Device> devices = new ArrayList<>();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported.");
            return devices;
        }

        if (!hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT)) {
            Log.e(TAG, "BLUETOOTH_CONNECT permission missing.");
            return devices;
        }

        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                devices.add(new Device(getDeviceName(context, device), device.getAddress()));
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: BLUETOOTH_CONNECT permission missing", e);
        }
        return devices;
    }

    /** ✅ Get Available (Discovered) Devices */
    public List<Device> getAvailableDevices(Context context) {
        if (!hasPermission(context, Manifest.permission.BLUETOOTH_SCAN)) {
            Log.e(TAG, "BLUETOOTH_SCAN permission missing.");
            return new ArrayList<>();
        }
        return new ArrayList<>(discoveredDevices);
    }

    /** ✅ Start Bluetooth Discovery */
    public boolean startDiscovery(Context context) {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported.");
            return false;
        }

        if (!hasPermission(context, Manifest.permission.BLUETOOTH_SCAN)) {
            Log.e(TAG, "BLUETOOTH_SCAN permission missing.");
            return false;
        }

        try {
            if (bluetoothAdapter.isDiscovering()) {
                Log.d(TAG, "Stopping previous discovery before starting a new one...");
                bluetoothAdapter.cancelDiscovery();
            }

            discoveredDevices.clear();
            boolean started = bluetoothAdapter.startDiscovery();
            Log.d(TAG, "Bluetooth discovery started: " + started);
            return started;
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: BLUETOOTH_SCAN permission missing", e);
            return false;
        }
    }

    /** ✅ Stop Bluetooth Discovery */
    public void stopDiscovery(Context context) {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported.");
            return;
        }

        if (!hasPermission(context, Manifest.permission.BLUETOOTH_SCAN)) {
            Log.e(TAG, "BLUETOOTH_SCAN permission missing.");
            return;
        }

        try {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
                Log.d(TAG, "Bluetooth discovery stopped.");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: BLUETOOTH_SCAN permission missing", e);
        }
    }

    /** ✅ Add Discovered Device */
    public void addDiscoveredDevice(Context context, BluetoothDevice device) {
        if (device == null) return;

        String address = device.getAddress();
        String name = getDeviceName(context, device);

        if (discoveredDevices.stream().noneMatch(d -> d.getAddress().equals(address))) {
            discoveredDevices.add(new Device(name, address));
            Log.d(TAG, "Discovered device added: " + name + " [" + address + "]");
        }
    }

    /** ✅ Pair with a Device */
    public boolean pairDevice(Context context, BluetoothDevice device) {
        if (device == null) return false;

        try {
            Method method = device.getClass().getMethod("createBond");
            boolean paired = (boolean) method.invoke(device);
            Log.d(TAG, "Pairing " + (paired ? "successful" : "failed") + " for " + getDeviceName(context, device));
            return paired;
        } catch (Exception e) {
            Log.e(TAG, "Error pairing device: " + e.getMessage(), e);
            return false;
        }
    }

    /** ✅ Unpair a Device */
    public boolean unpairDevice(Context context, BluetoothDevice device) {
        if (device == null) return false;

        try {
            Method method = device.getClass().getMethod("removeBond");
            boolean unpaired = (boolean) method.invoke(device);
            Log.d(TAG, "Unpairing " + (unpaired ? "successful" : "failed") + " for " + getDeviceName(context, device));
            return unpaired;
        } catch (Exception e) {
            Log.e(TAG, "Error unpairing device: " + e.getMessage(), e);
            return false;
        }
    }

    /** ✅ Get Device Name Safely */
    private String getDeviceName(Context context, BluetoothDevice device) {
        if (device == null) return "Unknown Device";

        String deviceName = "Unknown Device";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT)) {
                try {
                    deviceName = (device.getName() != null) ? device.getName() : "Unknown Device";
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException: Missing BLUETOOTH_CONNECT permission", e);
                }
            } else {
                Log.e(TAG, "BLUETOOTH_CONNECT permission missing. Using default name.");
            }
        } else {
            try {
                deviceName = (device.getName() != null) ? device.getName() : "Unknown Device";
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException: Unable to get device name", e);
            }
        }

        return deviceName;
    }

    /** ✅ Check Bluetooth Permissions */
    private boolean hasPermission(Context context, String permission) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }
}
