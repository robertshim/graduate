package com.example.rober.hereisdangerous;

import android.app.Application;

import io.realm.Realm;


public class ApplicationController extends Application {
    private String uid;
    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUid() {
        return uid;
    }
}
