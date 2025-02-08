package com.example.healthmonitoringapp.model;

public class Device {
    private String name;
    private String address;

    // Constructor
    public Device(String name, String address) {
        this.name = name;
        this.address = address;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    // Optionally, you can also override `toString()` for easier debugging
    @Override
    public String toString() {
        return "Device{name='" + name + "', address='" + address + "'}";
    }
}
