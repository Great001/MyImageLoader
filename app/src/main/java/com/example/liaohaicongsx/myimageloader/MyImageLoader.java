package com.example.liaohaicongsx.myimageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by liaohaicongsx on 2017/05/04.
 */
public class MyImageLoader {

    public static final String TAG = "MyImageLoader";

    public static final String CACHE_DIR = "bitmap";
    public static final long DISK_CACHE_SIZE = 50 * 1024 * 1024; //50M

    private LruCache<String, Bitmap> mMemoryCache;   //内存缓存
    private DiskLruCache mDiskCache;       //磁盘缓存

    private ExecutorService mExecutors;
    private Handler handler = new Handler(Looper.getMainLooper());

    private volatile static MyImageLoader instance;

    public MyImageLoader(Context context) {

        long maxMemory = Runtime.getRuntime().maxMemory();
        int maxCache = (int) (maxMemory / 8) / 1024;

        mMemoryCache = new LruCache<String, Bitmap>(maxCache) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight() / 1024;
            }
        };

        String cacheDir = getBitmapCacheDir(context);
        try {
            mDiskCache = DiskLruCache.open(new File(cacheDir), 1, 1, DISK_CACHE_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mExecutors = Executors.newFixedThreadPool(5);
    }

    public static MyImageLoader getInstance(Context context) {
        if (instance == null) {
            synchronized (MyImageLoader.class) {
                instance = new MyImageLoader(context);
            }
        }
        return instance;
    }


    // 整体流程是先在内存缓存中找，
    // 然后再去磁盘缓存中找，
    // 若都没有，则进行网络加载，并把网络加载的结果保存在内存和磁盘缓存中
    public void displayImage(String imgUrl, ImageView imageView) {
        if (displayImageFromMemoryCache(imgUrl, imageView) == null) {
            displayImageFromDiskCache(imgUrl, imageView);
        }
    }

    public Bitmap displayImageFromMemoryCache(String imgUrl, ImageView imageView) {
        Bitmap bitmap = mMemoryCache.get(imgUrl);
        if (bitmap != null) {
            Log.d(TAG,"from memory");
            imageView.setImageBitmap(bitmap);
        }
        return bitmap;
    }


    public Bitmap displayImageFromDiskCache(String imgUrl, ImageView imageView) {
        try {
            DiskLruCache.Snapshot snapshot = mDiskCache.get(Md5Util.generateMd5(imgUrl));
            if (snapshot != null) {
                FileInputStream fileInputStream = (FileInputStream) snapshot.getInputStream(0);
                FileDescriptor fd = fileInputStream.getFD();
                int reqWidth = imageView.getWidth();
                int reqHeight = imageView.getHeight();
                Bitmap bitmap = ImageResizer.getInstance().decodeSampledBitmapFromFD(fd, reqWidth, reqHeight);
                if (bitmap != null) {
                    Log.d(TAG,"from disk");
                    imageView.setImageBitmap(bitmap);
                    addBitmapToMemoryCache(imgUrl, bitmap);
                }
                return bitmap;
            } else {
                loadImageFromHttp(imageView, imgUrl);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (mMemoryCache.get(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }


    public void loadImageFromHttp(final ImageView imageView, final String imgUrl) {
        //通过HttpUrlConnection进行图片加载,需要利用多线程
        //开启一个异步线程进行网络加载
        //方案一：使用AsyncTask
/*
        AsyncTask<Object[],Integer,Bitmap> asyncTask = new AsyncTask<Object[], Integer, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Object[]... params) {
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
                        DiskLruCache.Editor editor = mDiskCache.edit(Md5Util.generateMd5(imgUrl));
                        if (editor != null) {
                            outputStream = editor.newOutputStream(0);
                            inputStream = httpURLConnection.getInputStream();
                            int len = -1;
                            while ((len = inputStream.read()) != -1) {
                                outputStream.write(len);
                            }
                            editor.commit();   //不要遗忘了
                            outputStream.flush();
                        }
                    }else{
                        Log.e(TAG,"返回码：" + httpURLConnection.getResponseCode() + "");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    httpURLConnection.disconnect();
                    if(inputStream != null){
                        try {
                            inputStream.close();  //关闭输入流
                        }catch (IOException e){

                        }
                    }
                    if(outputStream != null){
                        try{
                            outputStream.close();  //关闭输出流
                        }catch (IOException e){

                        }
                    }

                }
                return null;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                displayImageFromDiskCache(imgUrl, imageView);
            }
        };
        asyncTask.execute();
        //考虑一下AsyncTask的任务取消终止*/

        //方案二：使用java提供的线程池，尽管AsyncTask底层就是通过线程池实现的
        //有个问题：为什么加载出来的有些显示不出来呢?


        mExecutors.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG,Thread.currentThread().getId()+ "");
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
                        DiskLruCache.Editor editor = mDiskCache.edit(Md5Util.generateMd5(imgUrl));
                        if (editor != null) {
                            outputStream = editor.newOutputStream(0);
                            inputStream = httpURLConnection.getInputStream();
                            int len = -1;
                            while ((len = inputStream.read()) != -1) {
                                outputStream.write(len);
                            }
                            editor.commit();   //不要遗忘了
                            outputStream.flush();
                            Log.d(TAG,"from http");
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(TAG,"哈哈");
                                    displayImageFromDiskCache(imgUrl,imageView);
                                }
                            });
                        }
                    }else{
                        Log.e(TAG,"返回码：" + httpURLConnection.getResponseCode() + "");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    httpURLConnection.disconnect();
                    if(inputStream != null){
                        try {
                            inputStream.close();  //关闭输入流
                        }catch (IOException e){

                        }
                    }
                    if(outputStream != null){
                        try{
                            outputStream.close();  //关闭输出流
                        }catch (IOException e){

                        }
                    }

                }
            }
        });
    }

    public String getBitmapCacheDir(Context context) {
        String cacheRoot;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            cacheRoot = context.getExternalCacheDir().getPath();
        } else {
            cacheRoot = context.getCacheDir().getPath();
        }
        return cacheRoot + File.separator + CACHE_DIR + File.separator;
    }
}
