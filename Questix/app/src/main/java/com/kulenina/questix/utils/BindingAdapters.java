package com.kulenina.questix.utils;

import android.widget.ImageView;
import androidx.databinding.BindingAdapter;

public class BindingAdapters {

    @BindingAdapter("avatarResource")
    public static void setAvatarResource(ImageView imageView, String avatarName) {
        if (avatarName == null) {
            avatarName = "avatar_1";
        }

        int resourceId = imageView.getContext().getResources().getIdentifier(
            avatarName, "drawable", imageView.getContext().getPackageName()
        );

        if (resourceId != 0) {
            imageView.setImageResource(resourceId);
        } else {
            // Fallback to default avatar
            int defaultResourceId = imageView.getContext().getResources().getIdentifier(
                "avatar_1", "drawable", imageView.getContext().getPackageName()
            );
            if (defaultResourceId != 0) {
                imageView.setImageResource(defaultResourceId);
            }
        }
    }
}
