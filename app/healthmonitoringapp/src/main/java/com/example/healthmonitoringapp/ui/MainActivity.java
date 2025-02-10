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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 1;

    private EditText searchDevice;
    private RecyclerView pairedDevicesRecyclerView, newDevicesRecyclerView;
    private BluetoothAdapter bluetoothAdapter;
    private DevicesAdapter pairedDevicesAdapter, newDevicesAdapter;
    private List<Device> pairedDevicesList, newDevicesList, filteredDevicesList;
    private boolean isReceiverRegistered = false;

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

        checkPermissionsAndInitialize();
        setupSearchFilter();
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

        pairedDevicesAdapter = new DevicesAdapter(pairedDevicesList, device -> {
            Toast.makeText(this, "Clicked: " + device.getName(), Toast.LENGTH_SHORT).show();
        });

        newDevicesAdapter = new DevicesAdapter(filteredDevicesList, device -> {
            Toast.makeText(this, "Clicked: " + device.getName(), Toast.LENGTH_SHORT).show();
        });

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
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
            return;
        }

        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            pairedDevicesList.clear();
            if (pairedDevices != null && !pairedDevices.isEmpty()) {
                for (BluetoothDevice device : pairedDevices) {
                    pairedDevicesList.add(new Device(getSafeDeviceName(device), device.getAddress()));
                }
                pairedDevicesAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "No paired devices found", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission required to access paired devices", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
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
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                return (device.getName() != null) ? device.getName() : "Unknown Device";
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return "Unknown Device";
    }

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && hasBluetoothPermissions()) {
                    String deviceName = getSafeDeviceName(device);
                    String deviceAddress = device.getAddress();

                    boolean isAlreadyInList = newDevicesList.stream().anyMatch(d -> d.getAddress().equals(deviceAddress));

                    if (!isAlreadyInList) {
                        newDevicesList.add(new Device(deviceName, deviceAddress));
                        filterDevices(searchDevice.getText().toString());
                    }
                }
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        stopBluetoothDiscovery();
        unregisterReceiverSafely();
    }

    private void stopBluetoothDiscovery() {
        if (bluetoothAdapter != null && hasBluetoothPermissions()) {
            try {
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    private void unregisterReceiverSafely() {
        if (isReceiverRegistered) {
            unregisterReceiver(bluetoothReceiver);
            isReceiverRegistered = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted(grantResults)) {
                initializeBluetooth();
            } else {
                Toast.makeText(this, "Permissions not granted. Please enable them in settings.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean allPermissionsGranted(int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    private void setupSearchFilter() {
        searchDevice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterDevices(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterDevices(String query) {
        filteredDevicesList.clear();
        filteredDevicesList.addAll(newDevicesList.stream()
                .filter(device -> device.getName().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList()));
        newDevicesAdapter.notifyDataSetChanged();
    }
}
