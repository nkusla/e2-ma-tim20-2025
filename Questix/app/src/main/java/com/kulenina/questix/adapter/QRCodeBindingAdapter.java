package com.kulenina.questix.adapter;

import android.graphics.Bitmap;
import android.widget.ImageView;

import androidx.databinding.BindingAdapter;

import com.kulenina.questix.utils.QRCodeHelper;

public class QRCodeBindingAdapter {


    @BindingAdapter("qrUserId")
    public static void setQRCode(ImageView imageView, String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            imageView.setImageBitmap(null);
            return;
        }

        new Thread(() -> {
            Bitmap qrBitmap = QRCodeHelper.generateQRCode(userId);

            if (imageView.getContext() != null) {
                imageView.post(() -> {
                    if (qrBitmap != null) {
                        imageView.setImageBitmap(qrBitmap);
                    } else {
                        imageView.setImageBitmap(null);
                    }
                });
            }
        }).start();
    }
}
