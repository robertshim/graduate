package com.example.rober.hereisdangerous;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothService extends Service {
    private final int PENDING_INTENT = 10;
    private BluetoothAdapter adapter;
    private DatabaseReference reference;
    private List<BluetoothDeviceInfo> storedDevices;
    private String uid;
    private NotificationManager manager;
    private NotificationCompat.Builder builder;
    private ServiceThread serviceThread;
    private Handler handler;
    @Override
    public void onCreate() {
        super.onCreate();
        uid = ((ApplicationController)getApplicationContext()).getUid();
        reference = FirebaseDatabase.getInstance().getReference();
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                storedDevices = new ArrayList<>();
                for(DataSnapshot snapshot : dataSnapshot.child("users").child(uid).child("devices").getChildren()){
                    BluetoothDeviceInfo info = snapshot.getValue(BluetoothDeviceInfo.class);
                    storedDevices.add(info);
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

        serviceThread = new ServiceThread();
        handler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        adapter = BluetoothAdapter.getDefaultAdapter();

        serviceThread.start();
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

    private class ServiceThread extends Thread{
        private BluetoothServerSocket serverSocket;
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        @Override
        public void run() {
            try{
                serverSocket = adapter.listenUsingRfcommWithServiceRecord("Notification",uuid);
                BluetoothSocket socket = serverSocket.accept();
                byte[] bytes = new byte[1024];
                socket.getInputStream().read(bytes);
                String address = new String(bytes);
                for(BluetoothDeviceInfo item : storedDevices){
                    if(address.compareTo(item.address) == 0){
                        handler.post(() -> {
                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                intent.putExtra("location", item.location);
                                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), PENDING_INTENT, intent,PendingIntent.FLAG_UPDATE_CURRENT);
                                builder.setContentTitle(item.location);
                                builder.setContentText("알림발생");
                                builder.setContentIntent(pendingIntent);
                                manager.notify(222,builder.build());
                            });
                        break;
                    }
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

}
