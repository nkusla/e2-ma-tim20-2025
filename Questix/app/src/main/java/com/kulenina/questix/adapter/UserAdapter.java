package com.kulenina.questix.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kulenina.questix.R;
import com.kulenina.questix.model.User;
import com.kulenina.questix.viewmodel.UserViewModel;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<UserViewModel> userViewModels;
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public UserAdapter(List<UserViewModel> userViewModels, OnUserClickListener listener) {
        this.userViewModels = userViewModels;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserViewModel userViewModel = userViewModels.get(position);
        holder.bind(userViewModel, listener);
    }

    @Override
    public int getItemCount() {
        return userViewModels.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageViewAvatar;
        private TextView textViewUsername;
        private TextView textViewLevel;
        private TextView textViewXP;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewAvatar = itemView.findViewById(R.id.imageViewAvatar);
            textViewUsername = itemView.findViewById(R.id.textViewUsername);
            textViewLevel = itemView.findViewById(R.id.textViewLevel);
            textViewXP = itemView.findViewById(R.id.textViewXP);
        }

        public void bind(UserViewModel userViewModel, OnUserClickListener listener) {
            textViewUsername.setText(userViewModel.getUsername());
            textViewLevel.setText("Level " + userViewModel.getLevel());
            textViewXP.setText(userViewModel.getXp() + " XP");

            // Set avatar using UserViewModel's getAvatar() method
            setAvatarImage(userViewModel.getAvatar());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(userViewModel.getUser());
                }
            });
        }

        private void setAvatarImage(String avatar) {
            if (avatar == null || avatar.isEmpty()) {
                imageViewAvatar.setImageResource(R.drawable.avatar_1);
                return;
            }

            // Map avatar string to drawable resource
            switch (avatar) {
                case "avatar_1":
                    imageViewAvatar.setImageResource(R.drawable.avatar_1);
                    break;
                case "avatar_2":
                    imageViewAvatar.setImageResource(R.drawable.avatar_2);
                    break;
                case "avatar_3":
                    imageViewAvatar.setImageResource(R.drawable.avatar_3);
                    break;
                case "avatar_4":
                    imageViewAvatar.setImageResource(R.drawable.avatar_4);
                    break;
                case "avatar_5":
                    imageViewAvatar.setImageResource(R.drawable.avatar_5);
                    break;
                default:
                    imageViewAvatar.setImageResource(R.drawable.avatar_1);
                    break;
            }
        }
    }
}
