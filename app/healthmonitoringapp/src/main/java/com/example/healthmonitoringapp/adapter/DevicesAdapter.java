package com.example.healthmonitoringapp.adapter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthmonitoringapp.R;
import com.example.healthmonitoringapp.model.Device;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder> {
    private static final String TAG = "DevicesAdapter";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final List<Device> deviceList;
    private final Context context;
    private final OnDeviceClickListener listener;
    private final boolean isPairedList;
    private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    public DevicesAdapter(Context context, List<Device> devices, OnDeviceClickListener listener, boolean isPairedList) {
        this.context = context;
        this.deviceList = (devices != null) ? devices : new ArrayList<>();
        this.listener = listener;
        this.isPairedList = isPairedList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Device device = deviceList.get(position);
        holder.bind(device, listener);
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    /**
     * ✅ Swipe-to-Delete support
     */
    public void removeItem(int position) {
        if (position >= 0 && position < deviceList.size()) {
            deviceList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, deviceList.size());
        }
    }

    public void updateList(List<Device> newDevices) {
        if (newDevices == null) return;
        deviceList.clear();
        deviceList.addAll(newDevices);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView, addressTextView;
        private final ProgressBar loadingIndicator;

        public ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.deviceName);
            addressTextView = itemView.findViewById(R.id.deviceAddress);
            loadingIndicator = itemView.findViewById(R.id.loadingIndicator);
        }

        @SuppressLint("MissingPermission")
        public void bind(Device device, OnDeviceClickListener listener) {
            nameTextView.setText(device.getName() != null ? device.getName() : "Unknown Device");
            addressTextView.setText(device.getAddress() != null ? device.getAddress() : "No Address");

            // ✅ Highlight connected devices
            itemView.setBackgroundColor(ContextCompat.getColor(context,
                    device.isConnected() ? android.R.color.holo_green_light : android.R.color.white));

            // ✅ Handle device click (Connect)
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    loadingIndicator.setVisibility(View.VISIBLE);
                    listener.onDeviceClick(device, isPairedList);
                    connectToDevice(device);
                }
            });

            // ✅ Handle long press to unpair
            itemView.setOnLongClickListener(v -> {
                unpairDevice(device);
                return true;
            });
        }

        private void connectToDevice(Device device) {
            new Thread(() -> {
                BluetoothSocket socket = null;
                try {
                    if (!hasBluetoothPermissions()) {
                        showToast("Bluetooth permission required");
                        updateUI(false);
                        return;
                    }

                    BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.getAddress());

                    if (!hasConnectPermission()) {
                        showToast("Permission denied: Cannot connect to Bluetooth device.");
                        updateUI(false);
                        return;
                    }

                    socket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);

                    if (hasScanPermission()) {
                        bluetoothAdapter.cancelDiscovery();
                    }

                    socket.connect();

                    // ✅ Update device status
                    device.setConnected(true);
                    showToast("Connected to " + device.getName());
                    updateUI(true);

                } catch (SecurityException se) {
                    Log.e(TAG, "Permission denied: " + se.getMessage());
                    showToast("Permission denied: Cannot connect to Bluetooth device.");
                    updateUI(false);
                } catch (IOException e) {
                    Log.e(TAG, "Connection failed: " + e.getMessage());
                    showToast("Failed to connect to " + device.getName());
                    updateUI(false);
                    closeSocket(socket);
                }
            }).start();
        }

        private void unpairDevice(Device device) {
            try {
                if (!hasBluetoothPermissions()) {
                    showToast("Bluetooth permission required to unpair device.");
                    return;
                }

                BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.getAddress());

                if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Method removeBond = bluetoothDevice.getClass().getMethod("removeBond");
                    boolean success = (boolean) removeBond.invoke(bluetoothDevice);

                    if (success) {
                        device.setPaired(false);
                        device.setConnected(false);
                        deviceList.remove(device);
                        notifyDataSetChanged();
                        showToast(device.getName() + " unpaired successfully");
                    } else {
                        showToast("Failed to unpair " + device.getName());
                    }
                }
            } catch (SecurityException e) {
                showToast("Permission denied: Unable to unpair device.");
            } catch (Exception e) {
                Log.e(TAG, "Error while unpairing device: " + e.getMessage());
                showToast("Error while unpairing device.");
            }
        }

        private boolean hasBluetoothPermissions() {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }

        private boolean hasConnectPermission() {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }

        private boolean hasScanPermission() {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        }

        private void showToast(String message) {
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            );
        }

        private void updateUI(boolean success) {
            new Handler(Looper.getMainLooper()).post(() -> loadingIndicator.setVisibility(View.GONE));
        }

        private void closeSocket(BluetoothSocket socket) {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    Log.e(TAG, "Error closing socket: " + ex.getMessage());
                }
            }
        }
    }

    public interface OnDeviceClickListener {
        void onDeviceClick(Device device, boolean isPaired);
    }
}
