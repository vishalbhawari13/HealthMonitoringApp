package com.example.healthmonitoringapp.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.healthmonitoringapp.model.Device;

import java.util.List;
import java.util.Set;

public class BluetoothScanService {
    private static final String TAG = "BluetoothScanService";
    private final Context context;
    private final Activity activity;
    private final List<Device> deviceList;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback scanCallback;

    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 101;

    public BluetoothScanService(Context context, Activity activity, List<Device> deviceList) {
        this.context = context;
        this.activity = activity;
        this.deviceList = deviceList;

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = (bluetoothManager != null) ? bluetoothManager.getAdapter() : null;

        if (bluetoothAdapter != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
    }

    /**
     * ✅ Start Bluetooth Device Discovery (Supports both Classic & BLE)
     */
    @SuppressLint("MissingPermission")
    public void startBluetoothScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is not available or not enabled");
            return;
        }

        // ✅ Request Bluetooth permissions for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_BLUETOOTH_PERMISSIONS);
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && bluetoothLeScanner != null) {
            // ✅ Start BLE Scanning
            scanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    BluetoothDevice device = result.getDevice();
                    if (device.getName() == null || device.getAddress() == null) return; // Ignore unnamed devices

                    Log.d(TAG, "BLE Device Found: " + device.getName() + " [" + device.getAddress() + "]");
                    updateDeviceList(device, "BLE");
                }
            };

            bluetoothLeScanner.startScan(scanCallback);
            Log.d(TAG, "Bluetooth LE scan started...");
        } else {
            // ✅ Start Classic Bluetooth Discovery
            Log.d(TAG, "Starting Classic Bluetooth discovery...");
            bluetoothAdapter.startDiscovery();

            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                updateDeviceList(device, "Classic Bluetooth");
            }
        }
    }

    /**
     * ✅ Stop Bluetooth Scanning
     */
    @SuppressLint("MissingPermission")
    public void stopBluetoothScan() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && scanCallback != null && bluetoothLeScanner != null) {
                bluetoothLeScanner.stopScan(scanCallback);
                Log.d(TAG, "Bluetooth LE scan stopped.");
            }
            bluetoothAdapter.cancelDiscovery(); // ✅ Stop Classic Bluetooth Discovery
        }
    }

    /**
     * ✅ Add device to list safely (on UI thread)
     */
    private void updateDeviceList(BluetoothDevice device, String type) {
        new Handler(Looper.getMainLooper()).post(() -> {
            String deviceName = "Unknown Device";
            String deviceAddress = "00:00:00:00:00:00";

            // ✅ Check BLUETOOTH_CONNECT permission before accessing device name & address
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                if (device.getName() != null) {
                    deviceName = device.getName();
                }
                deviceAddress = device.getAddress();
            } else {
                Log.w(TAG, "Missing BLUETOOTH_CONNECT permission, cannot access device name & address.");
            }

            // ✅ Creating Device object safely
            Device newDevice = new Device(deviceName, deviceAddress, false, -100, false, type, "Unknown", System.currentTimeMillis());

            // ✅ Add to list if it's not already present
            if (!deviceList.contains(newDevice)) {
                deviceList.add(newDevice);
            }
        });
    }

}
