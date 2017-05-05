package com.example.liaohaicongsx.myimageloader;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ListView mLvTest;
    private LvAdapter mAdapter;
    private List<String> imgUrls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLvTest = (ListView) findViewById(R.id.lv_test);

        imgUrls = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            imgUrls.add("http://i4.buimg.com/1949/529b4b774033e3e7.png");
            imgUrls.add("http://i4.buimg.com/1949/ec8c18f5c0db7365.png");
            imgUrls.add("http://i1.piimg.com/1949/1ddef0087f8fa148.png");
        }

        mAdapter = new LvAdapter(this, imgUrls);
        mLvTest.setAdapter(mAdapter);


    }

}
