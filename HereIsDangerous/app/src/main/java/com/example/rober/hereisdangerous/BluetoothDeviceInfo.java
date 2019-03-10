package com.example.rober.hereisdangerous;


import android.os.Parcel;
import android.os.Parcelable;

class BluetoothDeviceInfo implements Parcelable {
    String address;
    String location;
    int distance;
    String uri;

    public BluetoothDeviceInfo(){}

    protected BluetoothDeviceInfo(Parcel in) {
        address = in.readString();
        location = in.readString();
        distance = in.readInt();
        uri = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(address);
        dest.writeString(location);
        dest.writeInt(distance);
        dest.writeString(uri);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BluetoothDeviceInfo> CREATOR = new Creator<BluetoothDeviceInfo>() {
        @Override
        public BluetoothDeviceInfo createFromParcel(Parcel in) {
            return new BluetoothDeviceInfo(in);
        }

        @Override
        public BluetoothDeviceInfo[] newArray(int size) {
            return new BluetoothDeviceInfo[size];
        }
    };
}
