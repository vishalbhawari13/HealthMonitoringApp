package com.example.healthmonitoringapp.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 1;

    private EditText searchDevice;
    private RecyclerView pairedDevicesRecyclerView, newDevicesRecyclerView;
    private BluetoothAdapter bluetoothAdapter;
    private DevicesAdapter pairedDevicesAdapter, newDevicesAdapter;
    private List<Device> pairedDevicesList, newDevicesList, filteredDevicesList;
    private boolean isReceiverRegistered = false; // Prevents unregister crash

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchDevice = findViewById(R.id.searchDevice);
        pairedDevicesRecyclerView = findViewById(R.id.pairedDevicesRecyclerView);
        newDevicesRecyclerView = findViewById(R.id.newDevicesRecyclerView);

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

        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
        } else {
            initializeBluetooth();
        }

        setupSearchFilter();
    }

    private void initializeBluetooth() {
        pairedDevicesList = new ArrayList<>();
        newDevicesList = new ArrayList<>();
        filteredDevicesList = new ArrayList<>();

        pairedDevicesAdapter = new DevicesAdapter(pairedDevicesList);
        newDevicesAdapter = new DevicesAdapter(newDevicesList);

        pairedDevicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        newDevicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        pairedDevicesRecyclerView.setAdapter(pairedDevicesAdapter);
        newDevicesRecyclerView.setAdapter(newDevicesAdapter);

        showPairedDevices();
        discoverNewDevices();
    }

    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, BLUETOOTH_PERMISSION_REQUEST_CODE);
        }
    }

    private void showPairedDevices() {
        if (hasBluetoothPermissions()) {
            try {
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                pairedDevicesList.clear();
                if (pairedDevices != null && !pairedDevices.isEmpty()) {
                    for (BluetoothDevice device : pairedDevices) {
                        String deviceName = getSafeDeviceName(device);
                        String deviceAddress = device.getAddress();
                        pairedDevicesList.add(new Device(deviceName, deviceAddress));
                    }
                    pairedDevicesAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(this, "No paired devices found", Toast.LENGTH_SHORT).show();
                }
            } catch (SecurityException e) {
                Toast.makeText(this, "Permission required to access paired devices", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else {
            requestBluetoothPermissions();
        }
    }

    private void discoverNewDevices() {
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
            return;
        }

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothReceiver, filter);
        isReceiverRegistered = true;

        try {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            bluetoothAdapter.startDiscovery();
        } catch (SecurityException e) {
            Toast.makeText(this, "Bluetooth discovery requires permission.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private String getSafeDeviceName(BluetoothDevice device) {
        String deviceName = "Unknown Device";
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                deviceName = (device.getName() != null) ? device.getName() : "Unknown Device";
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return deviceName;
    }

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && hasBluetoothPermissions()) {
                    String deviceName = getSafeDeviceName(device);
                    String deviceAddress = device.getAddress();

                    boolean isAlreadyPaired = pairedDevicesList.stream().anyMatch(d -> d.getAddress().equals(deviceAddress));
                    boolean isAlreadyInList = newDevicesList.stream().anyMatch(d -> d.getAddress().equals(deviceAddress));

                    if (!isAlreadyPaired && !isAlreadyInList) {
                        newDevicesList.add(new Device(deviceName, deviceAddress));
                        newDevicesAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        if (bluetoothAdapter != null && hasBluetoothPermissions()) {
            try {
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        if (isReceiverRegistered) {
            unregisterReceiver(bluetoothReceiver);
            isReceiverRegistered = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                initializeBluetooth();
            } else {
                Toast.makeText(this, "Permissions not granted. Please enable them in settings.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupSearchFilter() {
        searchDevice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                newDevicesAdapter.updateList(filterDevices(s.toString()));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private List<Device> filterDevices(String query) {
        List<Device> filteredList = new ArrayList<>();
        for (Device device : newDevicesList) {
            if (device.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(device);
            }
        }
        return filteredList;
    }
}
