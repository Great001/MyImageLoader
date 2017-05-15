package com.example.liaohaicongsx.myimageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by liaohaicongsx on 2017/05/04.
 */

/**
 * 轻量级图片加载类，包含图片内存缓存，磁盘缓存，网络加载，图片压缩
 */
public class MyImageLoader {

    public static final String TAG = "MyImageLoader";

    public static final String CACHE_DIR = "bitmap";
    /**
     * 磁盘缓存大小
     */
    public static final long DISK_CACHE_SIZE = 50 * 1024 * 1024; //50M
    /**
     * 内存缓存
     */
    private LruCache<String, Bitmap> mMemoryCache;
    /**
     * 磁盘缓存
     */
    private DiskLruCache mDiskCache;

    private ExecutorService mExecutors;
    private Handler mHandler = new Handler(Looper.getMainLooper());

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
    public void displayImage(final String imgUrl, final ImageView imageView) {
        Log.d(TAG, "begin");
        imageView.setTag(Md5Util.generateMd5(imgUrl));
        if (displayImageFromMemoryCache(imgUrl, imageView) == null) {
            mExecutors.execute(new Runnable() {
                @Override
                public void run() {
                    displayImageFromDiskCache(imgUrl, imageView);
                }
            });
        }

    }


    /**
     * 从内存缓存中加载显示图片
     * @param imgUrl
     * @param imageView
     * @return
     */
    public Bitmap displayImageFromMemoryCache(String imgUrl, ImageView imageView) {
        Log.d(TAG, imgUrl + " test memory");
        Bitmap bitmap = mMemoryCache.get(imgUrl);
        if (bitmap != null) {
            Log.d(TAG, imgUrl + " from memory");
            if(imageView.getTag().equals(Md5Util.generateMd5(imgUrl))) {
                imageView.setImageBitmap(bitmap);
            }
        }
        return bitmap;
    }

    /**
     * 从磁盘缓存加载显示图片
     * @param imgUrl
     * @param imageView
     * @return
     */
    public Bitmap displayImageFromDiskCache(final String imgUrl, final ImageView imageView) {
        Log.d(TAG, imgUrl + " test disk");
        try {
            DiskLruCache.Snapshot snapshot = mDiskCache.get(Md5Util.generateMd5(imgUrl));
            if (snapshot != null) {
                FileInputStream fileInputStream = (FileInputStream) snapshot.getInputStream(0);
                FileDescriptor fd = fileInputStream.getFD();
                int reqWidth = imageView.getWidth();
                int reqHeight = imageView.getHeight();
                final Bitmap bitmap = ImageResizer.getInstance().decodeSampledBitmapFromFD(fd, reqWidth, reqHeight);
                if (bitmap != null) {
                    Log.d(TAG, imgUrl + " from disk");
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, imgUrl + "success");
                            if(imageView.getTag().equals(Md5Util.generateMd5(imgUrl))) {
                                imageView.setImageBitmap(bitmap);
                            }
                        }
                    });
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

    /**
     * 添加bitamp进入内存缓存
     * @param key
     * @param bitmap
     */
    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (mMemoryCache.get(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }


    /**
     * 网络加载图片
     * @param imageView
     * @param imgUrl
     */
    public void loadImageFromHttp(final ImageView imageView, final String imgUrl) {
        try {
            DiskLruCache.Editor editor = mDiskCache.edit(Md5Util.generateMd5(imgUrl));
            if (editor != null) {
                Log.d(TAG, imgUrl + " test http");
                OutputStream outputStream = editor.newOutputStream(0);
                if (downUrlToStream(imgUrl, outputStream)) {
                    Log.d(TAG, imgUrl + " from http");
                    editor.commit();
                } else {
                    editor.abort();
                }
                mDiskCache.flush();
                Log.d(TAG, "SUCCESS_one");
                displayImageFromDiskCache(imgUrl, imageView);
            } else {
                Log.d(TAG, "SUCCESS_two");
                try {
                    //冲突等待
                    Thread.currentThread().sleep(1000);
                } catch (InterruptedException E) {
                    E.printStackTrace();
                }
                displayImageFromDiskCache(imgUrl, imageView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将网络加载的图片资源转换为流
     * @param imgUrl
     * @param outputStream
     * @return
     */
    public boolean downUrlToStream(String imgUrl, OutputStream outputStream) {

        HttpURLConnection httpURLConnection = null;
        BufferedInputStream inputStream = null;
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);

        try {
            URL url = new URL(imgUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setConnectTimeout(15000);
            httpURLConnection.setReadTimeout(15000);
            httpURLConnection.setDoInput(true);
            httpURLConnection.connect();

            if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                inputStream = new BufferedInputStream(httpURLConnection.getInputStream());
                int len = -1;
                while ((len = inputStream.read()) != -1) {
                    bufferedOutputStream.write(len);
                }
                bufferedOutputStream.flush();
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            httpURLConnection.disconnect();
            if (inputStream != null) {
                try {
                    inputStream.close();  //关闭输入流
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bufferedOutputStream != null) {
                try {
                    outputStream.close();  //关闭输出流
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return false;
    }

    /**
     * 获取磁盘缓存的目录
     * @param context
     * @return
     */
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