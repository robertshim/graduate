package com.example.rober.hereisdangerous;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public class BluetoothSocketObject {
    public BluetoothDevice device;
    public BluetoothSocket socket;

    public BluetoothSocketObject(BluetoothDevice device, BluetoothSocket socket) {
        this.device = device;
        this.socket = socket;
    }
}
