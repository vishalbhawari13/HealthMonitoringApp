package com.example.healthmonitoringapp.model;

import java.util.Objects;

public class Device implements Comparable<Device> {
    private String name;
    private String address;
    private boolean isPaired;
    private boolean isConnected;
    private int rssi; // Signal Strength Indicator (dBm)
    private String deviceType; // Classic Bluetooth or BLE
    private String deviceCategory; // Headset, Smartwatch, etc.
    private long lastConnectedTime; // Track last connected timestamp

    // ✅ Default Constructor (Safe Defaults)
    public Device() {
        this("Unknown Device", "00:00:00:00:00:00", false, -100, false, "Unknown", "Unknown", System.currentTimeMillis());
    }

    // ✅ Constructor for Name & Address Only
    public Device(String name, String address) {
        this(name, address, false, -100, false, "Unknown", "Unknown", System.currentTimeMillis());
    }

    // ✅ Full Constructor (Advanced Features)
    public Device(String name, String address, boolean isPaired, int rssi, boolean isConnected, String deviceType, String deviceCategory, long lastConnectedTime) {
        this.name = validateName(name);
        this.address = validateAddress(address);
        this.isPaired = isPaired;
        this.isConnected = isConnected;
        this.rssi = validateRssi(rssi);
        this.deviceType = validateType(deviceType);
        this.deviceCategory = validateType(deviceCategory);
        this.lastConnectedTime = (lastConnectedTime > 0) ? lastConnectedTime : System.currentTimeMillis();
    }

    // ✅ Getters
    public String getName() { return name; }
    public String getAddress() { return address; }
    public boolean isPaired() { return isPaired; }
    public boolean isConnected() { return isConnected; }
    public int getRssi() { return rssi; }
    public String getDeviceType() { return deviceType; }
    public String getDeviceCategory() { return deviceCategory; }
    public long getLastConnectedTime() { return lastConnectedTime; }

    // ✅ Setters with Validation
    public void setName(String name) { this.name = validateName(name); }
    public void setAddress(String address) { this.address = validateAddress(address); }
    public void setPaired(boolean paired) { this.isPaired = paired; }
    public void setConnected(boolean connected) {
        this.isConnected = connected;
        if (connected) this.lastConnectedTime = System.currentTimeMillis(); // Auto-update last connection time
    }
    public void setRssi(int rssi) { this.rssi = validateRssi(rssi); }
    public void setDeviceType(String deviceType) { this.deviceType = validateType(deviceType); }
    public void setDeviceCategory(String deviceCategory) { this.deviceCategory = validateType(deviceCategory); }
    public void setLastConnectedTime(long lastConnectedTime) {
        this.lastConnectedTime = (lastConnectedTime > 0) ? lastConnectedTime : System.currentTimeMillis();
    }

    // ✅ Validation Methods
    private String validateName(String name) {
        return (name != null && !name.trim().isEmpty()) ? name : "Unknown Device";
    }

    private String validateAddress(String address) {
        return (address != null && address.matches("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$")) ? address : "00:00:00:00:00:00";
    }

    private int validateRssi(int rssi) {
        return Math.max(-100, Math.min(rssi, 0)); // Clamp RSSI between -100 and 0
    }

    private String validateType(String type) {
        return (type != null && !type.trim().isEmpty()) ? type : "Unknown";
    }

    // ✅ Signal Strength Category
    public String getSignalStrengthCategory() {
        if (rssi >= -50) return "Strong";
        if (rssi >= -70) return "Medium";
        if (rssi >= -90) return "Weak";
        return "Very Weak";
    }

    // ✅ Sorting by RSSI (Higher Signal Strength First)
    @Override
    public int compareTo(Device other) {
        return Integer.compare(other.rssi, this.rssi); // Sort in descending order
    }

    // ✅ Debugging & Logging
    @Override
    public String toString() {
        return "Device{" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", isPaired=" + isPaired +
                ", isConnected=" + isConnected +
                ", rssi=" + rssi + " dBm (" + getSignalStrengthCategory() + ")" +
                ", deviceType='" + deviceType + '\'' +
                ", deviceCategory='" + deviceCategory + '\'' +
                ", lastConnectedTime=" + lastConnectedTime +
                '}';
    }

    // ✅ Ensuring Unique Devices in Lists (Comparison based on MAC Address)
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Device device = (Device) obj;
        return Objects.equals(address, device.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }
}
