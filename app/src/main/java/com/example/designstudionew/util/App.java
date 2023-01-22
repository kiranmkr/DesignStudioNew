package com.example.designstudionew.util;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.example.designstudionew.billing.GBilling;

public class App extends Application {

    public static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        Log.d("myApplication", "onCreate App");

        GBilling.initializeInAppClass(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

}
