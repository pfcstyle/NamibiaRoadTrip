package com.pfc.namibiaroadtrip;

import android.app.Application;

import com.pfc.namibiaroadtrip.utils.ErisImageLoader;

/**
 * Created by 58 on 2016/9/7.
 */
public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ErisImageLoader.getInstance().init(this);
    }
}
