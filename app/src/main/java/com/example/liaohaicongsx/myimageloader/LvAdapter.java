package com.example.liaohaicongsx.myimageloader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liaohaicongsx on 2017/05/05.
 */
public class LvAdapter extends BaseAdapter {

    private Context context;
    private List<String> imgUrls = new ArrayList<>();

    public LvAdapter(Context context, List<String> list) {
        this.context = context;
        imgUrls = list;
    }


    @Override
    public int getCount() {
        return imgUrls.size();
    }

    @Override
    public Object getItem(int position) {
        return imgUrls.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MyViewHolder holder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.lv_item, null);
            holder = new MyViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (MyViewHolder) convertView.getTag();
        }
        holder.tvNum.setText(position + "");
        MyImageLoader.getInstance(context).displayImage(imgUrls.get(position), holder.ivImg);
        return convertView;
    }

    class MyViewHolder {
        TextView tvNum;
        ImageView ivImg;

        MyViewHolder(View itemView) {
            tvNum = (TextView) itemView.findViewById(R.id.tv_test);
            ivImg = (ImageView) itemView.findViewById(R.id.iv_test);
        }

    }

}
