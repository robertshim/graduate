package com.example.rober.hereisdangerous;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

class StoredItemHolder extends RecyclerView.ViewHolder {
    ImageView imageView;
    ImageView btn_pencil;
    SwitchCompat btn_alarm;
    TextView location;
    StoredItemHolder(@NonNull View itemView) {
        super(itemView);

        imageView = itemView.findViewById(R.id.imageView);
        btn_pencil = itemView.findViewById(R.id.btn_pencil);
        btn_alarm = itemView.findViewById(R.id.btn_alarm);
        location = itemView.findViewById(R.id.location);
    }
}
