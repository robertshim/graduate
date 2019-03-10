package com.example.rober.hereisdangerous;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class SettingItemActivity extends AppCompatActivity {
    private BluetoothDeviceInfo info;
    private EditText edit_loc;
    private SeekBar seekBar;
    private TextView btn_ok;
    private ImageView btn_camera;
    private ImageView edit_imageView;
    private Handler handler;
    private DatabaseReference reference;
    private StorageReference storageReference;
    private String uid;
    private File filePath;
    private Uri photoURI;
    private int reqHeight;
    private int reqWidth;
    private byte[] bytes;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_item);
        info = getIntent().getParcelableExtra("item");
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        edit_loc = findViewById(R.id.edit_location);
        seekBar = findViewById(R.id.seekBar);
        btn_ok = findViewById(R.id.btn_ok);
        btn_camera = findViewById(R.id.btn_camera);
        edit_imageView = findViewById(R.id.edit_imageView);

        edit_loc.setText(info.location);
        seekBar.setProgress(info.distance - 2);

        handler = new Handler();

        btn_ok.setOnClickListener((view) -> {
            info.location = edit_loc.getText().toString();
            info.distance = seekBar.getProgress() + 2;
            new ClientThread().start();
        });


        btn_camera.setOnClickListener((view) ->{
            ImageCapture();
        });

        makeTempFile();
        reference = FirebaseDatabase.getInstance().getReference();
        SharedPreferences preferences = getSharedPreferences("UID",MODE_PRIVATE);
        uid = preferences.getString("uid",null);
        StorageReference reference = FirebaseStorage.getInstance().getReference().child(uid).child(info.address);
        RequestManager manager = Glide.with(this);
        reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                manager.load(uri).into(edit_imageView);
            }
        });

    }


    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    private boolean checkFilePermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},200);
            return false;
        }
        return true;
    }

    private boolean checkCameraPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA },300);
            return false;
        }
        return true;
    }

    private void makeTempFile(){
        if(checkFilePermission())
        {
            String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/MyCapture";
            File dir = new File(dirPath);
            if(!dir.exists()){
                dir.mkdir();
            }

            try{
                filePath = File.createTempFile("IMG",".jpg",dir);
                if(!filePath.exists())
                    filePath.createNewFile();
            }
            catch (Exception io){

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 200 && grantResults.length > 0){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                makeTempFile();
            }
        }
        if(requestCode == 300 && grantResults.length > 0){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                ImageCapture();
            }
        }
    }

    private void ImageCapture(){
        if(checkCameraPermission())
        {
            photoURI = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider",filePath);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(intent, 40);

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK)
        {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try{

                InputStream in = new FileInputStream(filePath);
                BitmapFactory.decodeStream(in, null, options);
                in.close();

            }catch(Exception io){
                io.printStackTrace();
            }

            final int height = options.outHeight;
            final int width = options.outWidth;
            reqHeight = 300;
            reqWidth = 150;
            int inSampleSize = 1;
            if(height > reqHeight || width > reqWidth){
                final int heightRatio = Math.round((float)height / (float) reqHeight);
                final int widthRatio = Math.round((float)width / (float) reqWidth);

                inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
            }

            BitmapFactory.Options imgOptions = new BitmapFactory.Options();
            imgOptions.inSampleSize = inSampleSize;

            Bitmap bitmap = BitmapFactory.decodeFile(filePath.getAbsolutePath(), imgOptions);
            edit_imageView.setImageBitmap(bitmap);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            //bitmap.compress(Bitmap.CompressFormat.PNG, 50, baos);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            bytes = baos.toByteArray();
        }
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
                    reference.child("devices").child(uid).child(info.address).setValue(info);
                    storageReference.child(uid).child(info.address).putFile(photoURI).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    setResult(RESULT_OK);
                                    finish();
                                }
                            });
                        }
                    });

                }else{
                    if(bytes != null)
                    {
                        storageReference.child(uid).child(info.address).putBytes(bytes).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                info.uri = "";
                                reference.child("devices").child(uid).child(info.address).setValue(info);
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        setResult(RESULT_OK);
                                        finish();
                                    }
                                });
                            }
                        });

                    }
                }
            }catch (IOException e){
                e.printStackTrace();
            }


        }
    }
}
