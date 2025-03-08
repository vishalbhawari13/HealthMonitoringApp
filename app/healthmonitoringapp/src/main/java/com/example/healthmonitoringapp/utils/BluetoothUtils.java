package com.example.healthmonitoringapp.utils;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Set;

public class BluetoothUtils {

    private static final String TAG = "BluetoothUtils";

    /**
     * ✅ Checks if Bluetooth is supported on the device.
     */
    public static boolean isBluetoothSupported(Context context) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        return bluetoothManager != null && bluetoothManager.getAdapter() != null;
    }

    /**
     * ✅ Checks if Bluetooth is enabled.
     */
    public static boolean isBluetoothEnabled() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    /**
     * ✅ Enables Bluetooth using ActivityResultLauncher (Replaces deprecated startActivityForResult)
     */
    public static void enableBluetooth(Activity activity, ActivityResultLauncher<Intent> enableBluetoothLauncher) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported.");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            // ✅ Check BLUETOOTH_CONNECT permission for Android 12+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (!hasPermission(activity, Manifest.permission.BLUETOOTH_CONNECT)) {
                    Log.e(TAG, "BLUETOOTH_CONNECT permission required to enable Bluetooth.");
                    requestBluetoothPermissions(activity);
                    return;
                }
            }

            // ✅ Launch Bluetooth enable request
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBluetoothLauncher.launch(enableBtIntent);
        }
    }

    /**
     * ✅ Returns a set of paired Bluetooth devices.
     */
    public static Set<BluetoothDevice> getPairedDevices(Context context) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported.");
            return null;
        }

        // ✅ Check BLUETOOTH_CONNECT permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT)) {
                Log.e(TAG, "Missing BLUETOOTH_CONNECT permission.");
                return null;
            }
        }

        try {
            return bluetoothAdapter.getBondedDevices(); // ✅ Safe to call after permission check
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: Bluetooth permission denied!", e);
            return null;
        }
    }


    /**
     * ✅ Checks if a device supports BLE or Classic Bluetooth.
     */
    public static String getBluetoothDeviceType(Context context, BluetoothDevice device) {
        if (device == null) return "Unknown Device";

        // ✅ Check BLUETOOTH_CONNECT permission for Android 12+ (API 31+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT)) {
                Log.e(TAG, "Missing BLUETOOTH_CONNECT permission.");
                return "Permission Required";
            }
        }

        try {
            int deviceType = device.getType(); // ✅ Safe inside try-catch block
            switch (deviceType) {
                case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                    return "Classic Bluetooth (SPP)";
                case BluetoothDevice.DEVICE_TYPE_LE:
                    return "Bluetooth Low Energy (BLE)";
                case BluetoothDevice.DEVICE_TYPE_DUAL:
                    return "Dual Mode (Classic + BLE)";
                default:
                    return "Unknown Type";
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: Bluetooth permission denied!", e);
            return "Permission Required";
        }
    }


    /**
     * ✅ Checks if the required Bluetooth permissions are granted.
     */
    private static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * ✅ Requests necessary Bluetooth permissions.
     */
    public static void requestBluetoothPermissions(Activity activity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1001);
        }
    }
}
