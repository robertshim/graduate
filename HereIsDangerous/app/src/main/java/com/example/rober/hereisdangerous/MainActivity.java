package com.example.rober.hereisdangerous;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        adapter = new StoredItemAdapter();
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
                info.distance = 0;
                intent.putExtra("item",info);
                startActivityForResult(intent,SETTING_REQUEST);
            }else{
                showToast("새로운 기기가 없습니다.");
            }

        });


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        reference = FirebaseDatabase.getInstance().getReference();
        uid = ((ApplicationController)getApplicationContext()).getUid();

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                storedDevices = new ArrayList<>();

                for(DataSnapshot snapshot : dataSnapshot.child("users").child(uid).child("devices").getChildren()){
                    BluetoothDeviceInfo info = snapshot.getValue(BluetoothDeviceInfo.class);
                    storedDevices.add(info);
                }

                adapter.addItems(storedDevices);
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

        Intent intent = new Intent(this, BluetoothService.class);
        startService(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        searchDevices();
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
                    if(newItems.size() > 0 ){
                        newItems.remove(0);
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
        int newCount;
        if(devices.size() > 0){
            if(storedDevices.size() > 0){
                Iterator<BluetoothDevice> iter = devices.iterator();
                int count = 0;
                while(iter.hasNext()){
                    BluetoothDevice d = iter.next();
                    String address = d.getAddress();
                    for(BluetoothDeviceInfo info : storedDevices){
                        if(address.compareTo(info.address) == 0){
                            count++;
                            devices.remove(d);
                            break;
                        }
                    }
                }

                newCount = devices.size() - count;
            }else{
                newCount = devices.size();
            }

            if(newCount > 0){
                notiText.setText(String.valueOf(newCount));
                notiText.setVisibility(View.VISIBLE);
                newItems.addAll(devices);
            }
        }
    }

}
