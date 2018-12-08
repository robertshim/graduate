package com.example.rober.hereisdangerous;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class StoredItemAdapter extends RecyclerView.Adapter<StoredItemHolder> {
    private List<BluetoothDeviceInfo> items;
    private Context context;
    @NonNull
    @Override
    public StoredItemHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        context = viewGroup.getContext();
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.stored_item, viewGroup, false);
        return new StoredItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoredItemHolder storedItemHolder, int i) {
        storedItemHolder.location.setText(items.get(i).location);
        storedItemHolder.btn_pencil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, SettingItemActivity.class);
                intent.putExtra("item",items.get(i));
                context.startActivity(intent);
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

}
