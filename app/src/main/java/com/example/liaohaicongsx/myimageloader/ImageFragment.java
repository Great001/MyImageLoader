package com.example.liaohaicongsx.myimageloader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Created by liaohaicongsx on 2017/05/15.
 */
public class ImageFragment extends DialogFragment {

    public static final String KEY_IMG_URL = "imgUrl";

    public static ImageFragment newInstance(String imgUrl) {
        ImageFragment dialogFragment = new ImageFragment();
        Bundle bundle = new Bundle();
        bundle.putString(KEY_IMG_URL, imgUrl);
        dialogFragment.setArguments(bundle);
        return dialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String imgUrl = getArguments().getString(KEY_IMG_URL);
        ImageView imageView = new ImageView(getActivity());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        imageView.setLayoutParams(params);
        MyImageLoader.getInstance(getActivity().getApplicationContext()).displayImage(imgUrl, imageView);
        builder.setView(imageView);
        return builder.create();
    }
}
