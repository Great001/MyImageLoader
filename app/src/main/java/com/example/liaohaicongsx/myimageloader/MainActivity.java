package com.example.liaohaicongsx.myimageloader;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView1;
    private ImageView imageView2;
    private Handler handler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView1 = (ImageView) findViewById(R.id.iv_test1);
        imageView2 = (ImageView) findViewById(R.id.iv_test2);

        handler = new Handler();
        MyImageLoader.getInstance().displayImage("http://i4.buimg.com/1949/ec8c18f5c0db7365.png",imageView1);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                MyImageLoader.getInstance().displayImage("http://i4.buimg.com/1949/ec8c18f5c0db7365.png",imageView2);
            }
        },5000);

    }

    //计算图片压缩比例
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
