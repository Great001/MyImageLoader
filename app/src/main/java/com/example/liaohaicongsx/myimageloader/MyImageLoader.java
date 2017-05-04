package com.example.liaohaicongsx.myimageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by liaohaicongsx on 2017/05/04.
 */
public class MyImageLoader {

    public static final String TAG = "MyImageLoader";

    private LruCache<String, Bitmap> memoryCache;
    private DiskLruCache diskLruCache;

    public static final String CACHE_DIR = "bitmap";
    public static final long DISK_CACHE_SIZE = 50 * 1024 * 1024; //50M

    private volatile static MyImageLoader instance;

    public MyImageLoader(Context context) {
        long maxMemory = Runtime.getRuntime().maxMemory();
        int maxCache = (int) (maxMemory / 8) / 1024;
        memoryCache = new LruCache<String, Bitmap>(maxCache) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight() / 1024;
            }
        };

        String cacheDir = getBitmapCacheDir(context);
        try {
            diskLruCache = DiskLruCache.open(new File(cacheDir), 1, 1, DISK_CACHE_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static MyImageLoader getInstance(Context context) {
        if (instance == null) {
            synchronized (MyImageLoader.class) {
                instance = new MyImageLoader(context);
            }
        }
        return instance;
    }


    //整体流程是先在内存缓存中找，在去磁盘缓存中找，若都没有，则进行网络加载，并把网络加载的结果保存在内存和磁盘缓存中
    public void displayImage(String imgUrl, ImageView imageView) {
        if(displayImageFromMemoryCache(imgUrl,imageView) == null){
            displayImageFromDiskCache(imgUrl,imageView);
        }
    }

    public Bitmap displayImageFromMemoryCache(String imgUrl,ImageView imageView){
        Bitmap bitmap = memoryCache.get(imgUrl);
        imageView.setImageBitmap(bitmap);
        return bitmap;
    }


    public Bitmap displayImageFromDiskCache(String imgUrl,ImageView imageView){
        try{
            DiskLruCache.Snapshot snapshot = diskLruCache.get(imgUrl);
            FileInputStream fileInputStream = (FileInputStream) snapshot.getInputStream(0);
            FileDescriptor fd = fileInputStream.getFD();
            int reqWidth = imageView.getWidth();
            int reqHeight = imageView.getHeight();
            Bitmap bitmap = ImageResizer.getInstance().decodeSampledBitmapFromFD(fd,reqWidth,reqHeight);
            if(bitmap != null){
                addBitmapToMemoryCache(imgUrl,bitmap);
            }else{
                loadImageFromHttp(imageView,imgUrl);
            }
        }catch (IOException e){
            e.printStackTrace();
        }

        return null;
    }
    public void addBitmapToMemoryCache(String key,Bitmap bitmap){
        if(memoryCache.get(key) == null){
            memoryCache.put(key,bitmap);
        }
    }


    public void loadImageFromHttp(final ImageView imageView, final String imgUrl) {
        final Handler handler = new Handler(Looper.getMainLooper());
        //通过HttpUrlConnection进行图片加载,需要利用多线程
        //开启一个异步线程进行网络加载
        AsyncTask asyncTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                HttpURLConnection httpURLConnection = null;
                OutputStream outputStream = null;
                InputStream inputStream = null;
                try {
                    URL url = new URL(imgUrl);
                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setRequestMethod("GET");
                    httpURLConnection.setConnectTimeout(5000);
                    httpURLConnection.connect();

                    if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
//                        final Bitmap bitmap = BitmapFactory.decodeStream(httpURLConnection.getInputStream());
                        DiskLruCache.Editor editor = diskLruCache.edit(imgUrl);
                        outputStream = editor.newOutputStream(0);
                        inputStream = httpURLConnection.getInputStream();
                        int len = -1;
                        while((len = inputStream.read()) != -1) {
                            outputStream.write(len);
                        }
                        outputStream.flush();
                        inputStream.close();
                        outputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    httpURLConnection.disconnect();
                }
                return null;
            }
        };
        asyncTask.execute();
        //考虑一下AsyncTask的任务取消终止
    }

    public String getBitmapCacheDir(Context context) {
        String cacheRoot;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            cacheRoot = context.getExternalCacheDir().getPath();
        } else {
            cacheRoot = context.getCacheDir().getPath();
        }

        return cacheRoot + File.separator + CACHE_DIR + File.separator;

    }


}
