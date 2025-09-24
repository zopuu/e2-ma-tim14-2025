package com.example.habibitar;

import android.app.Application;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        com.example.habibitar.notify.NotificationsHub.get().init(this);
    }
}
