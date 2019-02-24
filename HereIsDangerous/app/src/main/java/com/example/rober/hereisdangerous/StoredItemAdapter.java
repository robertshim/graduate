package com.example.rober.hereisdangerous;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class StoredItemAdapter extends RecyclerView.Adapter<StoredItemHolder> {
    private List<BluetoothDeviceInfo> items;
    private Context context;
    private StorageReference rootReference;
    private String uid;
    public StoredItemAdapter(List<BluetoothDeviceInfo> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public StoredItemHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        context = viewGroup.getContext();
        uid = context.getSharedPreferences("UID",Context.MODE_PRIVATE).getString("uid",null);
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.stored_item, viewGroup, false);
        return new StoredItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoredItemHolder storedItemHolder, int i) {
        StorageReference reference = rootReference.child(items.get(i).address);
        reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(context).load(uri).into(storedItemHolder.imageView);
            }
        });
        storedItemHolder.location.setText(items.get(i).location);
        storedItemHolder.btn_pencil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, SettingItemActivity.class);
                intent.putExtra("item",items.get(i));
                context.startActivity(intent);
            }
        });
        storedItemHolder.btn_remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rootReference.child(items.get(i).address).delete();
                FirebaseDatabase.getInstance().getReference().child("devices").child(uid).child(items.get(i).address).removeValue();
                items.remove(i);
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addItems(List<BluetoothDeviceInfo> newItems){
        items = newItems;
        notifyDataSetChanged();
    }

    public void setRootReference(StorageReference reference){
        rootReference = reference;
    }
}
