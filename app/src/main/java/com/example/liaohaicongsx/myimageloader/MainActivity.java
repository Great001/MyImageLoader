package com.example.liaohaicongsx.myimageloader;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
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
        for (int i = 0; i < 10; i++) {
            imgUrls.add("http://i2.muimg.com/1949/732523acc12d5e27.jpg");
            imgUrls.add("http://i2.muimg.com/1949/170b75304ea82905.jpg");
            imgUrls.add("http://i2.muimg.com/1949/3aff8f1b5523beb7.jpg");
//            imgUrls.add("http://i2.muimg.com/1949/0f5cee8f7dd92b41.jpg");
//            imgUrls.add("http://i2.muimg.com/1949/5996c150cb12db75.jpg");
        }

        mAdapter = new LvAdapter(this, imgUrls);
        mLvTest.setAdapter(mAdapter);

        mLvTest.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ImageFragment.newInstance(imgUrls.get(position)).show(getFragmentManager(),"enen");
            }
        });
    }

}
