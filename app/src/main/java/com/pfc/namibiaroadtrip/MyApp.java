package com.pfc.namibiaroadtrip;

import android.app.Application;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.pfc.namibiaroadtrip.utils.EsriImageLoader;

/**
 * Created by pfc on 2016/9/7.
 */
public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        EsriImageLoader.getInstance().init(this);
        EsriImageLoader.setLoadFailedBitmap( BitmapFactory.decodeResource(getResources(), R.drawable.load_failed));
        EsriImageLoader.setLoadFailedBitmap( BitmapFactory.decodeResource(getResources(), R.drawable.loading));
    }
}
