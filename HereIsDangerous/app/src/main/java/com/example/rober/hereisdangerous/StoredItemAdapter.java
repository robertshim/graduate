package com.example.rober.hereisdangerous;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class StoredItemAdapter extends RecyclerView.Adapter<StoredItemHolder> {
    private List<BluetoothDeviceInfo> items;

    @NonNull
    @Override
    public StoredItemHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.stored_item, viewGroup, false);
        return new StoredItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoredItemHolder storedItemHolder, int i) {
        storedItemHolder.location.setText(items.get(i).location);
        
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
