package com.pfc.namibiaroadtrip.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;

import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.io.File;

/**
 * Created by pfc on 2016/9/7.
 */
public class EsriImageLoader {
    private static EsriImageLoader ourInstance = new EsriImageLoader();

    public static EsriImageLoader getInstance() {
        return ourInstance;
    }

    private EsriImageLoader() {
    }

    public static void setLoadFailedBitmap(Bitmap loadFailedBitmap) {
        EsriImageLoader.loadFailedBitmap = loadFailedBitmap;
    }

    public static void setLoadingBitmap(Bitmap loadingBitmap) {
        EsriImageLoader.loadingBitmap = loadingBitmap;
    }

    private static Bitmap loadFailedBitmap;
    private static Bitmap loadingBitmap;

    public void init(Context context){
        int memClass = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE))
                .getMemoryClass();
        int memoryCacheSize = (memClass / 8) * 1024 * 1024; // 1/8 of app
        File cacheDir = new File(FileCache.getCacheFolder(context, "",
                FileCache.CACHE_TYPE_SDCARD));

        DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true).cacheOnDisk(true)
                .imageScaleType(ImageScaleType.IN_SAMPLE_INT)
                .bitmapConfig(Bitmap.Config.RGB_565).build();
        ImageLoaderConfiguration configuration = new ImageLoaderConfiguration.Builder(
                context).denyCacheImageMultipleSizesInMemory()
                .threadPoolSize(3)
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .denyCacheImageMultipleSizesInMemory()
                .memoryCache(new LruMemoryCache(memoryCacheSize))
                .memoryCacheSize(10 * 1024 * 1024)
                .diskCache(
                        new UnlimitedDiscCache(cacheDir, null,
                                new FileNameGenerator() {
                                    @Override
                                    public String generate(String imageUri) {
                                        return imageUri.replace("/", "_");
                                    }
                                }))
                .tasksProcessingOrder(QueueProcessingType.LIFO)
                .defaultDisplayImageOptions(defaultOptions).build();
        ImageLoader.getInstance().init(configuration);
    }

    public void displayImage(String url, final ImageView imageView){
        System.out.println("url++++++++++++:"+url);
        ImageLoader.getInstance().loadImage(url, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String s, View view) {
                if (loadingBitmap != null) {
                    imageView.setImageBitmap(loadingBitmap);
                }
            }

            @Override
            public void onLoadingFailed(String s, View view, FailReason failReason) {
                if (loadFailedBitmap != null)
                imageView.setImageBitmap(loadFailedBitmap);
            }

            @Override
            public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                System.out.println("图片加载完成");
                imageView.setImageBitmap(bitmap);
            }

            @Override
            public void onLoadingCancelled(String s, View view) {

            }
        });
    }

}
