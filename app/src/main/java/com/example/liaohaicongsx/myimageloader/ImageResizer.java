package com.example.liaohaicongsx.myimageloader;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.FileDescriptor;

/**
 * Created by liaohaicongsx on 2017/05/04.
 */
public class ImageResizer {

    //实现图片高效加载

    private volatile static ImageResizer instance;

    public static ImageResizer getInstance() {
        if (instance == null) {
            synchronized (ImageResizer.class) {
                instance = new ImageResizer();
            }
        }
        return instance;
    }

    //图片的压缩处理
    //Dedcode FileDescriptor
    public Bitmap decodeSampledBitmapFromFD(FileDescriptor fd, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fd, null, options);
        int outWidth = options.outWidth;
        int outHeight = options.outHeight;
        options.inSampleSize = calSampleSize(outWidth, outHeight, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(fd, null, options);
    }


    //Decode Resources
    public Bitmap decodeSampledBitmapFromRes(Resources resources, int resId, int reqWidth, int reqHeight) {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resources, resId, options);
        int outWidth = options.outWidth;
        int outHeight = options.outHeight;
        options.inSampleSize = calSampleSize(outWidth, outHeight, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(resources, resId, options);
    }

    //计算图片压缩比例,即采样率
    public int calSampleSize(int reqWidth, int reqHeight, int outWidth, int outHeight) {

        int inSampleSize = 1;

//        int max = Math.min(outWidth/reqWidth,outHeight/reqHeight);
//        while(max >= 2){
//            max = max / inSampleSize;
//            inSampleSize = inSampleSize * 2;
//        }
        //因为是要2的整数倍，所以得进行特殊的处理
        if (outWidth == 0 || outHeight == 0) {
            return 1;
        }
        if (outWidth > reqWidth && outHeight > reqHeight) {

            int halfWidth = outWidth / 2;
            int halfHeight = outHeight / 2;

            while ((halfWidth / inSampleSize >= reqWidth) && (halfHeight / inSampleSize >= reqHeight)) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

}
