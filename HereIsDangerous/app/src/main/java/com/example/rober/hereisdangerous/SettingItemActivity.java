package com.example.rober.hereisdangerous;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class SettingItemActivity extends AppCompatActivity {
    private BluetoothDeviceInfo info;
    private EditText edit_loc;
    private SeekBar seekBar;
    private TextView btn_ok;
    private Handler handler;
    private DatabaseReference reference;
    private String uid;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_item);
        info = getIntent().getParcelableExtra("item");

        edit_loc = findViewById(R.id.edit_location);
        seekBar = findViewById(R.id.seekBar);
        btn_ok = findViewById(R.id.btn_ok);

        edit_loc.setText(info.location);
        seekBar.setProgress(info.distance);

        handler = new Handler();

        btn_ok.setOnClickListener((view) -> {
            info.location = edit_loc.getText().toString();
            info.distance = seekBar.getProgress() + 2;
            new ClientThread().start();
        });

        reference = FirebaseDatabase.getInstance().getReference();
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        uid = ((ApplicationController)getApplication()).getUid();
    }


    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }


    private class ClientThread extends Thread{
        private BluetoothAdapter bluetoothAdapter;

        ClientThread(){
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        @Override
        public void run() {
            BluetoothSocket socket;
            BluetoothDevice target = null;
            Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

            for(BluetoothDevice device : devices){
                if(info.address.compareTo(device.getAddress()) == 0){
                    target = device;
                    break;
                }
            }

            try {
                bluetoothAdapter.cancelDiscovery();
                if(target !=null){
                    socket = target.createRfcommSocketToServiceRecord(uuid);
                    socket.connect();
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(info.distance);
                    reference.child("users").child(uid).child("devices").setValue(info);
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}
