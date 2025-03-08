package com.example.healthmonitoringapp.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthmonitoringapp.R;
import com.example.healthmonitoringapp.adapter.DevicesAdapter;
import com.example.healthmonitoringapp.model.Device;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 1;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private EditText searchDevice;
    private RecyclerView pairedDevicesRecyclerView, newDevicesRecyclerView;
    private ProgressBar scanningProgressBar;
    private BluetoothAdapter bluetoothAdapter;
    private DevicesAdapter pairedDevicesAdapter, newDevicesAdapter;
    private List<Device> pairedDevicesList, newDevicesList, filteredDevicesList;
    private boolean isReceiverRegistered = false;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Handler handler;
    private Device currentPairingDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is not enabled", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        checkPermissionsAndInitialize();
    }

    private void initUI() {
        searchDevice = findViewById(R.id.searchDevice);
        pairedDevicesRecyclerView = findViewById(R.id.pairedDevicesRecyclerView);
        newDevicesRecyclerView = findViewById(R.id.newDevicesRecyclerView);
        scanningProgressBar = findViewById(R.id.scanningProgressBar);
    }

    private void checkPermissionsAndInitialize() {
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
        } else {
            initializeBluetooth();
        }
    }

    private void initializeBluetooth() {
        pairedDevicesList = new ArrayList<>();
        newDevicesList = new ArrayList<>();
        filteredDevicesList = new ArrayList<>();

        pairedDevicesAdapter = new DevicesAdapter(this, pairedDevicesList, this::connectToDevice, true);
        newDevicesAdapter = new DevicesAdapter(this, filteredDevicesList, this::pairAndConnectDevice, false);

        pairedDevicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        newDevicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        pairedDevicesRecyclerView.setAdapter(pairedDevicesAdapter);
        newDevicesRecyclerView.setAdapter(newDevicesAdapter);

        showPairedDevices();
        discoverNewDevices();
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(Device device, boolean showToast) {
        if (device == null || bluetoothAdapter == null) {
            if (showToast) {
                runOnUiThread(() -> Toast.makeText(this, "Invalid device or Bluetooth not available", Toast.LENGTH_SHORT).show());
            }
            return;
        }

        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.getAddress());

        new Thread(() -> {
            try {
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }

                if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                    bluetoothSocket.close();
                }

                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothSocket.connect();

                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();

                if (showToast) {
                    runOnUiThread(() -> Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show());
                }

                readDataFromDevice();
            } catch (IOException e) {
                if (showToast) {
                    runOnUiThread(() -> Toast.makeText(this, "Connection failed! Try again.", Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }


    @SuppressLint("MissingPermission")
    private void pairAndConnectDevice(Device device, boolean showToast) {
        if (device == null || bluetoothAdapter == null) {
            if (showToast) {
                runOnUiThread(() -> Toast.makeText(this, "Invalid device or Bluetooth not available", Toast.LENGTH_SHORT).show());
            }
            return;
        }

        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.getAddress());

        // If already paired, just connect
        if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            if (showToast) {
                runOnUiThread(() -> Toast.makeText(this, device.getName() + " is already paired. Connecting...", Toast.LENGTH_SHORT).show());
            }
            connectToDevice(device, showToast);
            return;
        }

        currentPairingDevice = device;

        try {
            boolean pairingStarted = bluetoothDevice.createBond();

            if (pairingStarted) {
                if (!isReceiverRegistered) {
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                    registerReceiver(pairingReceiver, filter);
                    isReceiverRegistered = true;
                }
                if (showToast) {
                    runOnUiThread(() -> Toast.makeText(this, "Pairing started with " + device.getName(), Toast.LENGTH_SHORT).show());
                }
            } else {
                if (showToast) {
                    runOnUiThread(() -> Toast.makeText(this, "Pairing failed!", Toast.LENGTH_SHORT).show());
                }
            }
        } catch (Exception e) {
            Log.e("Bluetooth", "Pairing error: " + e.getMessage());
            if (showToast) {
                runOnUiThread(() -> Toast.makeText(this, "Pairing error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }
    }


    // ✅ Fetch and display paired Bluetooth devices
    private void showPairedDevices() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Check for Bluetooth permissions (required for Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        pairedDevicesList.clear(); // ✅ Clear previous list

        if (!pairedDevices.isEmpty()) {
            for (BluetoothDevice device : pairedDevices) {
                // ✅ Convert BluetoothDevice to your custom Device model
                Device customDevice = new Device(device.getName(), device.getAddress());
                pairedDevicesList.add(customDevice);
            }
        }

        pairedDevicesAdapter.notifyDataSetChanged(); // ✅ Refresh RecyclerView
    }


    // ✅ Discover new Bluetooth devices
    private void discoverNewDevices() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Check for Bluetooth scan permission (required for Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1);
            return;
        }

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery(); // Stop any ongoing discovery
        }

        bluetoothAdapter.startDiscovery(); // ✅ Start device discovery
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(Device device) {
        if (device == null || bluetoothAdapter == null) {
            runOnUiThread(() -> Toast.makeText(this, "Invalid device or Bluetooth not available", Toast.LENGTH_SHORT).show());
            return;
        }

        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.getAddress());

        new Thread(() -> {
            try {
                // Ensure Bluetooth is enabled
                if (!bluetoothAdapter.isEnabled()) {
                    runOnUiThread(() -> Toast.makeText(this, "Bluetooth is turned off!", Toast.LENGTH_SHORT).show());
                    return;
                }

                // Stop discovery to prevent interference
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }

                // Close existing socket if connected
                if (bluetoothSocket != null) {
                    try {
                        bluetoothSocket.close();
                    } catch (IOException ignored) {
                    }
                    bluetoothSocket = null;
                }

                // Create & connect Bluetooth socket
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothSocket.connect();

                // Setup input/output streams
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();

                runOnUiThread(() -> {
                    Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
                    Log.d("Bluetooth", "Connected to: " + device.getName());
                });

                readDataFromDevice();

            } catch (IOException e) {
                Log.e("Bluetooth", "Connection failed: " + e.getMessage());

                try {
                    if (bluetoothSocket != null) {
                        bluetoothSocket.close();
                        bluetoothSocket = null;
                    }
                } catch (IOException ignored) {}

                runOnUiThread(() -> Toast.makeText(this, "Connection failed! Try again.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    private void readDataFromDevice() {
        handler = new Handler();
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    String receivedText = new String(buffer, 0, bytes);
                    runOnUiThread(() -> Toast.makeText(this, "Received: " + receivedText, Toast.LENGTH_SHORT).show());
                } catch (IOException e) {
                    break;
                }
            }
        }).start();
    }

    @SuppressLint("MissingPermission")
    private void pairAndConnectDevice(Device device) {
        if (device == null || bluetoothAdapter == null) {
            runOnUiThread(() -> Toast.makeText(this, "Invalid device or Bluetooth not available", Toast.LENGTH_SHORT).show());
            return;
        }

        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.getAddress());

        // Check if already paired
        if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            runOnUiThread(() -> Toast.makeText(this, device.getName() + " is already paired. Connecting...", Toast.LENGTH_SHORT).show());
            connectToDevice(device);
            return;
        }

        currentPairingDevice = device;

        try {
            boolean pairingStarted = bluetoothDevice.createBond();

            if (pairingStarted) {
                if (!isReceiverRegistered) {
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                    registerReceiver(pairingReceiver, filter);
                    isReceiverRegistered = true;
                }
                runOnUiThread(() -> Toast.makeText(this, "Pairing started with " + device.getName(), Toast.LENGTH_SHORT).show());
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Pairing failed!", Toast.LENGTH_SHORT).show());
            }
        } catch (Exception e) {
            Log.e("Bluetooth", "Pairing error: " + e.getMessage());
            runOnUiThread(() -> Toast.makeText(this, "Pairing error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }


    private final BroadcastReceiver pairingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device != null) {
                    // ✅ Check for Bluetooth permission before accessing bond state
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(context, "Bluetooth permission denied!", Toast.LENGTH_SHORT).show();
                        return; // Exit to prevent a SecurityException
                    }

                    if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        Toast.makeText(context, "Paired successfully with " + device.getName(), Toast.LENGTH_SHORT).show();

                        if (currentPairingDevice != null && currentPairingDevice.getAddress().equals(device.getAddress())) {
                            connectToDevice(currentPairingDevice); // ✅ Connect only if it's the same device
                        }
                    }
                }
            }
        }
    };


    private boolean hasBluetoothPermissions() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED);
    }

    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
            }, BLUETOOTH_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isReceiverRegistered) {
            unregisterReceiver(pairingReceiver);
        }
    }
}
