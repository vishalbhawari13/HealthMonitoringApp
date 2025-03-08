package com.example.healthmonitoringapp.services;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothService extends Service {
    private static final String TAG = "BluetoothService";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard UUID for SPP

    private final IBinder binder = new LocalBinder();
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean isConnected = false;

    private BluetoothDataListener dataListener;
    private HandlerThread handlerThread;
    private Handler backgroundHandler;
    private Handler mainHandler;

    public class LocalBinder extends Binder {
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        handlerThread = new HandlerThread("BluetoothThread");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());
        mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Interface for Bluetooth data callbacks.
     */
    public interface BluetoothDataListener {
        void onDataReceived(String data);
    }

    public void setBluetoothDataListener(BluetoothDataListener listener) {
        this.dataListener = listener;
    }

    /**
     * Connect to a Bluetooth device.
     */
    public void connectToDevice(Context context, BluetoothDevice device, ConnectionCallback callback) {
        if (isConnected) {
            Log.d(TAG, "Already connected.");
            callback.onFailure("Already connected to a device.");
            return;
        }

        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported.");
            callback.onFailure("Bluetooth not supported on this device.");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission.");
            callback.onFailure("Missing BLUETOOTH_CONNECT permission.");
            return;
        }

        backgroundHandler.post(() -> {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                bluetoothSocket.connect();
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
                isConnected = true;

                Log.d(TAG, "Connected to " + (device.getName() != null ? device.getName() : "Unknown Device"));
                mainHandler.post(callback::onSuccess);

                readDataFromDevice();
            } catch (IOException e) {
                Log.e(TAG, "Error connecting to device", e);
                mainHandler.post(() -> callback.onFailure("Error connecting to device: " + e.getMessage()));
                closeConnection();
            }
        });
    }

    /**
     * Read data from the connected Bluetooth device.
     */
    private void readDataFromDevice() {
        backgroundHandler.post(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                while (isConnected) {
                    String receivedData = reader.readLine();
                    Log.d(TAG, "Received: " + receivedData);

                    if (dataListener != null) {
                        mainHandler.post(() -> dataListener.onDataReceived(receivedData));
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading data", e);
                closeConnection();
            }
        });
    }

    /**
     * Send data to the connected Bluetooth device.
     */
    public void sendData(String data, DataSendCallback callback) {
        if (!isConnected || outputStream == null) {
            Log.e(TAG, "Not connected to a device.");
            callback.onFailure("No device connected.");
            return;
        }

        backgroundHandler.post(() -> {
            try {
                outputStream.write(data.getBytes());
                Log.d(TAG, "Sent: " + data);
                mainHandler.post(callback::onSuccess);
            } catch (IOException e) {
                Log.e(TAG, "Error sending data", e);
                mainHandler.post(() -> callback.onFailure("Error sending data: " + e.getMessage()));
            }
        });
    }

    /**
     * Disconnect from the Bluetooth device.
     */
    public void disconnectDevice() {
        backgroundHandler.post(this::closeConnection);
    }

    private void closeConnection() {
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
            isConnected = false;
            Log.d(TAG, "Disconnected.");
        } catch (IOException e) {
            Log.e(TAG, "Error disconnecting", e);
        }
    }

    /**
     * Check if the device is connected.
     */
    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeConnection();
        handlerThread.quitSafely();
    }

    /**
     * Callback interface for Bluetooth connection status.
     */
    public interface ConnectionCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    /**
     * Callback interface for sending data.
     */
    public interface DataSendCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }
}
