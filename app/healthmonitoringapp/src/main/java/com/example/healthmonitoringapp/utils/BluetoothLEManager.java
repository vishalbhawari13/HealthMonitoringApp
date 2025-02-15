package com.example.healthmonitoringapp.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothLEManager {
    private static final String TAG = "BluetoothLEManager";

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private final Handler handler = new Handler();

    private static final long SCAN_PERIOD = 10000; // 10 seconds

    // Heart Rate Service & Characteristic UUIDs (Modify based on your smartwatch)
    private static final UUID HEART_RATE_SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID HEART_RATE_CHAR_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");

    public BluetoothLEManager(Context context) {
        this.context = context;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = (bluetoothManager != null) ? bluetoothManager.getAdapter() : null;
        bluetoothLeScanner = (bluetoothAdapter != null) ? bluetoothAdapter.getBluetoothLeScanner() : null;
    }

    // 🔹 Check if BLE is supported
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    // 🔹 Start Scanning for BLE Devices
    @SuppressLint("MissingPermission")
    public void startScan() {
        if (bluetoothAdapter == null || bluetoothLeScanner == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is disabled or not available");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "BLUETOOTH_SCAN permission not granted.");
            return;
        }

        Log.d(TAG, "Starting BLE scan...");

        handler.postDelayed(() -> {
            bluetoothLeScanner.stopScan(scanCallback);
            Log.d(TAG, "Scan stopped.");
        }, SCAN_PERIOD);

        List<ScanFilter> filters = new ArrayList<>();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        bluetoothLeScanner.startScan(filters, settings, scanCallback);
    }

    // 🔹 Stop Scanning
    @SuppressLint("MissingPermission")
    public void stopScan() {
        if (bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(scanCallback);
            Log.d(TAG, "BLE Scan stopped.");
        }
    }

    // 🔹 BLE Scan Callback
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();

            // ✅ Check permissions before accessing device name and address
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Missing BLUETOOTH_SCAN or BLUETOOTH_CONNECT permission.");
                    return;
                }
            }

            // ✅ Check if the device and name are valid
            if (device != null && device.getName() != null) {
                Log.d(TAG, "Discovered Device: " + device.getName() + " [" + device.getAddress() + "]");
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Scan failed with error: " + errorCode);
        }
    };

    // 🔹 Connect to a BLE Device
    @SuppressLint("MissingPermission")
    public void connectToDevice(BluetoothDevice device) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "BLUETOOTH_CONNECT permission not granted.");
            return;
        }

        if (device == null) {
            Log.e(TAG, "Device is null, cannot connect.");
            return;
        }

        Log.d(TAG, "Connecting to device: " + device.getName() + " [" + device.getAddress() + "]");
        bluetoothGatt = device.connectGatt(context, false, gattCallback);
    }

    // 🔹 Disconnect & Cleanup
    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
            Log.d(TAG, "Disconnected from GATT server.");
        }
    }

    // 🔹 GATT Callback - Handles connection events
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.d(TAG, "Connected to GATT server.");

                // ✅ Check BLUETOOTH_CONNECT permission before calling discoverServices()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Missing BLUETOOTH_CONNECT permission.");
                    return;
                }

                gatt.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.e(TAG, "Disconnected from GATT server.");
                disconnect();
            }
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService heartRateService = gatt.getService(HEART_RATE_SERVICE_UUID);
                if (heartRateService != null) {
                    BluetoothGattCharacteristic heartRateChar = heartRateService.getCharacteristic(HEART_RATE_CHAR_UUID);
                    if (heartRateChar != null) {

                        // ✅ Check BLUETOOTH_CONNECT permission before setting notifications
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission.");
                            return;
                        }

                        gatt.setCharacteristicNotification(heartRateChar, true);
                        Log.d(TAG, "Heart Rate service discovered.");
                    } else {
                        Log.e(TAG, "Heart Rate characteristic not found.");
                    }
                } else {
                    Log.e(TAG, "Heart Rate service not found.");
                }
            } else {
                Log.e(TAG, "Service discovery failed with status: " + status);
            }
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (HEART_RATE_CHAR_UUID.equals(characteristic.getUuid())) {
                int heartRate = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
                Log.d(TAG, "Heart Rate: " + heartRate);
                // Update UI with heart rate data
            }
        }
    };
}
