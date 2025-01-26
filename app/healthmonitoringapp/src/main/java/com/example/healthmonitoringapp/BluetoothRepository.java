package com.example.healthmonitoringapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.List;

public class BluetoothRepository {

    private final BluetoothAdapter bluetoothAdapter;

    public BluetoothRepository() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public List<Device> getPairedDevices() {
        List<Device> devices = new ArrayList<>();
        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            devices.add(new Device(device.getName(), device.getAddress()));
        }
        return devices;
    }
}
