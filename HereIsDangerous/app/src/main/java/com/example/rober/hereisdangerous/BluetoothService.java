package com.example.rober.hereisdangerous;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BluetoothService extends Service {
    private final int PENDING_INTENT = 10;
    private BluetoothAdapter adapter;
    private DatabaseReference reference;
    private List<BluetoothDeviceInfo> storedDevicesInfo;
    private List<BluetoothDevice> storedDevices = new ArrayList<>();
    private String uid;
    private NotificationManager manager;
    private NotificationCompat.Builder builder;
    private Handler handler;
    private ServiceThread thread = null;
    private boolean flag = true;
    private List<BluetoothSocketObject> sockets;
    private Map<String, BluetoothDeviceInfo> storedDevicesMap;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("SsSsSs","서비스 실행");
        SharedPreferences preferences = getApplicationContext().getSharedPreferences("UID",MODE_PRIVATE);
        uid = preferences.getString("uid",null);
        String _uid = ((ApplicationController)getApplicationContext()).getUid();
        if(_uid != null){
            uid = _uid;
        }
        reference = FirebaseDatabase.getInstance().getReference();
        reference.child("devices").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                storedDevicesInfo = new ArrayList<>();
                for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                    BluetoothDeviceInfo info = snapshot.getValue(BluetoothDeviceInfo.class);
                    if(info !=null){
                        storedDevicesInfo.add(info);
                        storedDevicesMap.put(info.address, info);
                    }
                }
                Log.d("SsSsSs","파이어 베이스 실행");
                searchDevices();
                if(thread == null){
                    thread = new ServiceThread();
                    thread.start();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            String channelId = "one-channel";
            String channelName = "Bluetooth noti";
            String channelDescription = "Bluetooth notification channel";
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(channelDescription);
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100});
            manager.createNotificationChannel(channel);
            builder = new NotificationCompat.Builder(getApplicationContext(), channelId);
        }else{
            builder = new NotificationCompat.Builder(getApplicationContext());
        }

        builder.setSmallIcon(android.R.drawable.ic_notification_overlay);
        builder.setWhen(System.currentTimeMillis());
        builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
        builder.setAutoCancel(true);

        //new ServiceThread().start();
        handler = new Handler();
        adapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean stopService(Intent name) {
        return super.stopService(name);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void searchDevices(){
        Log.d("SsSsSs","블루투스 서치");
        Set<BluetoothDevice> devices = adapter.getBondedDevices();
        storedDevices = new ArrayList<>();
        if(devices.size() > 0){
            if(storedDevicesInfo.size() > 0) {
                Iterator<BluetoothDevice> iter = devices.iterator();

                while(iter.hasNext()) {
                    BluetoothDevice d = iter.next();
                    String address = d.getAddress();
                    Log.d("SsSsSs",address);
                    for (BluetoothDeviceInfo info : storedDevicesInfo) {
                        if (address.compareTo(info.address) == 0) {
                            storedDevices.add(d);
                            Log.d("SsSsSs","device add");
                            break;
                        }
                    }
                }
            }
        }
        flag = true;
    }


    private class ServiceThread extends Thread{
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        @Override
        public void run() {
            while(true){
                Log.d("SsSsSs","스레드 실행");
                if(flag){
                    sockets = new ArrayList<>();
                    for(BluetoothDevice d : storedDevices){
                        try{
                            BluetoothSocket socket = d.createRfcommSocketToServiceRecord(uuid);
                            socket.connect();
                            Log.d("SsSsSs","connect socket");
                            sockets.add(new BluetoothSocketObject(d,socket));
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                    flag = false;
                }

                for(BluetoothSocketObject object : sockets){
                    try{
                        Log.d("SsSsSs","소켓 확인중");
                        int len = object.socket.getInputStream().read();
                        //읽어오기
                        if(len != -1){
                            handler.post(() -> {
                                BluetoothDeviceInfo info = storedDevicesMap.get(object.device.getAddress());
                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                intent.putExtra("location", info.location);
                                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), PENDING_INTENT, intent,PendingIntent.FLAG_UPDATE_CURRENT);
                                builder.setContentTitle(info.location);
                                builder.setContentText("알림발생");
                                builder.setContentIntent(pendingIntent);
                                manager.notify(222,builder.build());
                            });
                        }
                    }catch (IOException e){
                        e.printStackTrace();
                    }

                }
                try{
                    sleep(1000L);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }

            }
        }
    }
}
