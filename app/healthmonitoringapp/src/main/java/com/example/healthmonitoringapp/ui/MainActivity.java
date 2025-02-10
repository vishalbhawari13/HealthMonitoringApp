package com.example.healthmonitoringapp.ui;

import com.example.healthmonitoringapp.utils.SwipeToDeleteCallback;
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
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthmonitoringapp.R;
import com.example.healthmonitoringapp.adapter.DevicesAdapter;
import com.example.healthmonitoringapp.model.Device;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 1;
    private static final UUID APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private EditText searchDevice;
    private RecyclerView pairedDevicesRecyclerView, newDevicesRecyclerView;
    private BluetoothAdapter bluetoothAdapter;
    private DevicesAdapter pairedDevicesAdapter, newDevicesAdapter;
    private List<Device> pairedDevicesList, newDevicesList;
    private boolean isReceiverRegistered = false;
    private Device connectedDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeUI();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            showToast("Bluetooth not supported");
            finish();
            return;
        }

        checkPermissionsAndInitialize();
    }

    private void initializeUI() {
        searchDevice = findViewById(R.id.searchDevice);
        pairedDevicesRecyclerView = findViewById(R.id.pairedDevicesRecyclerView);
        newDevicesRecyclerView = findViewById(R.id.newDevicesRecyclerView);

        pairedDevicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        newDevicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
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

        // ✅ Pass both connect & unpair listeners
        pairedDevicesAdapter = new DevicesAdapter(pairedDevicesList, this::connectToDevice, this::unpairDevice);
        newDevicesAdapter = new DevicesAdapter(newDevicesList, this::connectToDevice, null); // Unpair not needed for new devices

        pairedDevicesRecyclerView.setAdapter(pairedDevicesAdapter);
        newDevicesRecyclerView.setAdapter(newDevicesAdapter);

        showPairedDevices();
        discoverNewDevices();
        setupSwipeToDelete();
    }


    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
            }, BLUETOOTH_PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, BLUETOOTH_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeBluetooth();
        } else {
            showToast("Bluetooth permissions denied!");
        }
    }

    private void showPairedDevices() {
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
            return;
        }

        // ✅ Explicit permission check to prevent SecurityException
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            showToast("Bluetooth permission required to access paired devices");
            return;
        }

        pairedDevicesList.clear();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices != null && !pairedDevices.isEmpty()) {
            for (BluetoothDevice device : pairedDevices) {
                // ✅ Ensure correct constructor (passing RSSI as 0)
                pairedDevicesList.add(new Device(getDeviceName(device), device.getAddress(), 0));
            }
            pairedDevicesAdapter.notifyDataSetChanged();
        } else {
            showToast("No paired devices found");
        }
    }


    private void discoverNewDevices() {
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
            return;
        }

        // ✅ Explicit permission check for BLUETOOTH_SCAN
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            showToast("Bluetooth scan permission required to discover new devices");
            return;
        }

        if (isReceiverRegistered) {
            unregisterReceiver(bluetoothReceiver);
            isReceiverRegistered = false;
        }

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothReceiver, filter);
        isReceiverRegistered = true;

        bluetoothAdapter.startDiscovery(); // ✅ Now safe to call
    }


    private String getDeviceName(BluetoothDevice device) {
        // ✅ Check BLUETOOTH_CONNECT permission before accessing device name
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return "Unknown Device"; // ✅ Return default name if permission is missing
        }

        return (device.getName() != null) ? device.getName() : "Unknown Device";
    }


    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0); // ✅ Get RSSI value

                if (device != null && !isDeviceAlreadyListed(device.getAddress())) {
                    newDevicesList.add(new Device(getDeviceName(device), device.getAddress(), rssi)); // ✅ Pass RSSI
                    newDevicesAdapter.notifyDataSetChanged();
                }
            }
        }
    };


    private boolean isDeviceAlreadyListed(String deviceAddress) {
        for (Device device : newDevicesList) {
            if (device.getAddress().equals(deviceAddress)) {
                return true;
            }
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(Device device) {
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
            return;
        }

        showToast("Connecting to: " + device.getName());
        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.getAddress());

        try {
            BluetoothSocket socket = bluetoothDevice.createRfcommSocketToServiceRecord(APP_UUID);
            socket.connect();
            showToast("Connected to: " + device.getName());
            connectedDevice = device;
            pairedDevicesAdapter.setConnectedDevice(device);
        } catch (IOException e) {
            showToast("Failed to connect to " + device.getName());
        }
    }

    private void unpairDevice(Device device) {
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
            return;
        }

        try {
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.getAddress());
            bluetoothDevice.getClass().getMethod("removeBond").invoke(bluetoothDevice);
            showToast("Unpaired: " + device.getName());
            pairedDevicesList.remove(device);
            pairedDevicesAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            showToast("Failed to unpair " + device.getName());
        }
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback(pairedDevicesAdapter));
        itemTouchHelper.attachToRecyclerView(pairedDevicesRecyclerView);
    }


    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isReceiverRegistered) {
            unregisterReceiver(bluetoothReceiver);
            isReceiverRegistered = false;
        }
    }
}
