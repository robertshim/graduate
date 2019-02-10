package com.example.rober.hereisdangerous;

import android.bluetooth.BluetoothDevice;

public class BluetoothObject {
    public BluetoothObject(BluetoothDevice device, int distance) {
        this.device = device;
        this.distance = distance;
    }

    public BluetoothDevice device = null;
    public int distance  = -1;
}
