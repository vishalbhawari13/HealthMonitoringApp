package com.example.healthmonitoringapp.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import com.example.healthmonitoringapp.services.BluetoothScanService;

import java.util.ArrayList;
import java.util.List;

public class DeviceScanActivity extends AppCompatActivity implements DevicesAdapter.OnDeviceClickListener {
    private static final String TAG = "DeviceScanActivity";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 100;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothScanService bluetoothScanService;
    private DevicesAdapter devicesAdapter;

    private List<Device> deviceList = new ArrayList<>();
    private List<Device> filteredDevicesList = new ArrayList<>();

    private RecyclerView recyclerViewDevices;
    private ProgressBar progressBar;
    private Button btnScan, btnStopScan;
    private EditText searchDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);

        // Initialize UI components
        recyclerViewDevices = findViewById(R.id.recyclerViewDevices);
        progressBar = findViewById(R.id.progressBar);
        btnScan = findViewById(R.id.btnScan);
        btnStopScan = findViewById(R.id.btnStopScan);
        searchDevice = findViewById(R.id.searchDevice);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        devicesAdapter = new DevicesAdapter(this, filteredDevicesList, this, false);

        recyclerViewDevices.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDevices.setAdapter(devicesAdapter);

        bluetoothScanService = new BluetoothScanService(this, this, deviceList);

        checkBluetoothSupport();
        checkAndRequestBluetoothPermissions();

        btnScan.setOnClickListener(v -> startScan());
        btnStopScan.setOnClickListener(v -> stopScan());

        setupSearchFilter();
    }

    /**
     * ✅ Check and request Bluetooth permissions (for Android 12+)
     */
    private void checkAndRequestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT
                        },
                        REQUEST_BLUETOOTH_PERMISSIONS);
            }
        }
    }

    /**
     * ✅ Handle permission request results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Bluetooth permissions granted");
            } else {
                showToast("Bluetooth permissions denied. App may not work properly.");
            }
        }
    }

    /**
     * ✅ Check if Bluetooth is supported & enabled
     */
    private void checkBluetoothSupport() {
        if (bluetoothAdapter == null) {
            showToast("Bluetooth not supported on this device");
            finish();
        } else if (!bluetoothAdapter.isEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                showToast("Bluetooth connect permission required");
                return;
            }
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    /**
     * ✅ Start scanning for Bluetooth devices
     */
    @SuppressLint("MissingPermission")
    private void startScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            showToast("Permission denied: Cannot start Bluetooth scan.");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        bluetoothScanService.startBluetoothScan();
        showToast("Scanning for devices...");
    }

    /**
     * ✅ Stop Bluetooth scanning
     */
    private void stopScan() {
        progressBar.setVisibility(View.GONE);
        bluetoothScanService.stopBluetoothScan();
        showToast("Scan stopped.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothScanService.stopBluetoothScan();
    }

    /**
     * ✅ Handle device clicks
     */
    @Override
    public void onDeviceClick(Device device, boolean isPaired) {
        String status = isPaired ? "Paired" : "Unpaired";
        showToast("Clicked on: " + device.getName() + " (" + status + ")");
    }

    /**
     * ✅ Set up search filter for devices
     */
    private void setupSearchFilter() {
        searchDevice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Required method, but no action needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterDevices(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Required method, but no action needed
            }
        });
    }

    /**
     * ✅ Filter device list based on search query
     */
    private void filterDevices(String query) {
        filteredDevicesList.clear();
        for (Device device : deviceList) {
            if (device.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredDevicesList.add(device);
            }
        }
        devicesAdapter.notifyDataSetChanged();
    }

    /**
     * ✅ Utility function to show toast messages
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
