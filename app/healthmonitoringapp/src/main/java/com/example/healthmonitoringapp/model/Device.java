package com.example.healthmonitoringapp.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Device implements Parcelable {
    private String name;
    private String address;
    private int rssi; // Signal Strength Indicator

    // Constructor
    public Device(String name, String address, int rssi) {
        this.name = name;
        this.address = address;
        this.rssi = rssi;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public int getRssi() {
        return rssi;
    }

    // Setters (Optional)
    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    // Parcelable Implementation
    protected Device(Parcel in) {
        name = in.readString();
        address = in.readString();
        rssi = in.readInt();
    }

    public static final Creator<Device> CREATOR = new Creator<Device>() {
        @Override
        public Device createFromParcel(Parcel in) {
            return new Device(in);
        }

        @Override
        public Device[] newArray(int size) {
            return new Device[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(address);
        dest.writeInt(rssi);
    }

    // Improved toString() for debugging
    @Override
    public String toString() {
        return "Device{name='" + name + "', address='" + address + "', rssi=" + rssi + "}";
    }
}
