package com.example.rober.hereisdangerous;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.text.LocaleDisplayNames;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter;
    private final int BLUETOOTH_REQUEST = 5;
    private final int SETTING_REQUEST = 6;
    private final int PENDING_INTENT = 10;
    private DatabaseReference reference;
    private List<BluetoothDeviceInfo> storedDevices;
    private List<BluetoothDevice> newItems;
    private String uid;
    private RecyclerView recyclerView;
    private StoredItemAdapter adapter;
    private TextView notiText;
    private ConstraintLayout layout;
    private Map<String, BluetoothObject> sentObject;
    private List<BluetoothObject> sendObject;
    private Handler handler;
    private boolean flag = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);

        notiText = findViewById(R.id.notitext);
        layout = findViewById(R.id.bell);

        notiText.setVisibility(View.INVISIBLE);
        layout.setOnClickListener((view) -> {
            int count = Integer.valueOf(notiText.getText().toString());
            if(count > 0){
                Intent intent = new Intent(getApplicationContext(), SettingItemActivity.class);
                BluetoothDeviceInfo info = new BluetoothDeviceInfo();
                info.address = newItems.get(0).getAddress();
                info.location = "";
                info.distance = 2;
                intent.putExtra("item",info);
                startActivityForResult(intent,SETTING_REQUEST);
            }else{
                showToast("새로운 기기가 없습니다.");
            }

        });
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        SharedPreferences preferences = getSharedPreferences("UID",MODE_PRIVATE);
        uid = preferences.getString("uid",null);

        reference = FirebaseDatabase.getInstance().getReference();

        reference.child("devices").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                storedDevices = new ArrayList<>();
                //BluetoothDeviceInfo info = dataSnapshot.getValue(BluetoothDeviceInfo.class);
                for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                    BluetoothDeviceInfo info = snapshot.getValue(BluetoothDeviceInfo.class);
                    if(info !=null){
                        storedDevices.add(info);
                    }
                }
                Log.d("zzzzzz",String.valueOf(storedDevices.size()));
                adapter.addItems(storedDevices);
                searchDevices();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        if(bluetoothAdapter != null){

            if(!bluetoothAdapter.isEnabled()){
                Intent bIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(bIntent,BLUETOOTH_REQUEST);
            }
        }
        storedDevices = new ArrayList<>();
        adapter = new StoredItemAdapter(storedDevices);
        adapter.setRootReference(FirebaseStorage.getInstance().getReference().child(uid));
        sentObject = new HashMap<>();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        handler = new Handler();

    }

    private void serviceStart(){
        Intent intent = new Intent(this, BluetoothService.class);
        startService(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //searchDevices();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode){
            case BLUETOOTH_REQUEST:
                if(resultCode == RESULT_OK){
                    showToast("블루투스 활성화");
                }else{
                    showToast("앱을 사용하시려면 블루투스를 활성화 해주시기 바랍니다.");
                }
                break;

            case SETTING_REQUEST:
                if(resultCode == RESULT_OK){
                    if(newItems.size() == 1 ){
                        newItems.remove(0);
                        notiText.setVisibility(View.INVISIBLE);
                    }
                    else if(newItems.size() > 1){
                        newItems.remove(0);
                        notiText.setText(String.valueOf(newItems.size()));
                        notiText.setVisibility(View.INVISIBLE);
                    }
                }
                break;
            case PENDING_INTENT:
                //다이얼로그 만들어서 호출하기
                String location = data.getStringExtra("location");
                Toast.makeText(this, location+"에서 알림 발생", Toast.LENGTH_LONG).show();
                break;
        }
    }


    private void showToast(String string){
        Toast.makeText(getApplicationContext(),  string, Toast.LENGTH_LONG).show();
    }


    private void searchDevices(){
        Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
        newItems = new ArrayList<>(devices);
        int newCount = 0;
        sendObject = new ArrayList<>();
        if(devices.size() > 0){
            if(storedDevices.size() > 0){
                Iterator<BluetoothDevice> iter = devices.iterator();
                int count = 0;
                while(iter.hasNext()){
                    BluetoothDevice d = iter.next();
                    String address = d.getAddress();
                    int distance = -1;
                    for(BluetoothDeviceInfo info : storedDevices){
                        if(address.compareTo(info.address) == 0){
                            count++;
                            newItems.remove(d);
                            distance = info.distance;
                            break;
                        }
                    }

                    if(sentObject.get(address) == null){
                        sendObject.add(new BluetoothObject(d,distance));
                    }
                }

                newCount = newItems.size() - count;
            }else{
                newCount = newItems.size();
            }

            if(newCount > 0){
                notiText.setText(String.valueOf(newCount));
                notiText.setVisibility(View.VISIBLE);
                for(BluetoothDevice d : newItems){
                    if(sentObject.get(d.getAddress()) == null)
                        sendObject.add(new BluetoothObject(d,-1));
                }
            }else{
                notiText.setText("0");
                notiText.setVisibility(View.INVISIBLE);
            }
        }
        Log.d("zzzzzz",String.valueOf(newCount));
        new ClientThread().start();
    }

    private class ClientThread extends Thread{
        @Override
        public void run() {
             bluetoothAdapter.cancelDiscovery();
             UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
             for(BluetoothObject object : sendObject){
                 try{
                     BluetoothSocket socket = object.device.createRfcommSocketToServiceRecord(uuid);
                     socket.connect();
                     socket.getOutputStream().write(object.distance);
                     socket.close();
                     Log.d("nononono",String.valueOf(object.distance));
                 }catch (IOException e){
                     e.printStackTrace();
                 }
                 sentObject.put(object.device.getAddress(),object);
             }

             serviceStart();
        }
    }
}
