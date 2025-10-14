package com.kulenina.questix.adapter;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.databinding.BindingAdapter;

public class ColorBindingAdapters {

    @BindingAdapter("colorTint")
    public static void setColorTint(View view, String colorHex) {
        if (colorHex == null || colorHex.isEmpty()) return;
        try {
            int color = Color.parseColor(colorHex);
            Drawable drawable = view.getBackground();

            if (drawable != null) {
                drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            } else {
                view.setBackgroundColor(color);
            }
        } catch (IllegalArgumentException e) {
        }
    }
}