package com.kulenina.questix.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kulenina.questix.R;
import com.kulenina.questix.model.User;
import com.kulenina.questix.viewmodel.UserViewModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FriendSelectionAdapter extends RecyclerView.Adapter<FriendSelectionAdapter.FriendSelectionViewHolder> {
    private List<UserViewModel> friendViewModels;
    private Set<String> selectedFriendIds;
    private OnFriendSelectionChangeListener listener;

    public interface OnFriendSelectionChangeListener {
        void onSelectionChanged(Set<String> selectedFriendIds);
    }

    public FriendSelectionAdapter(List<UserViewModel> friendViewModels, OnFriendSelectionChangeListener listener) {
        this.friendViewModels = friendViewModels;
        this.listener = listener;
        this.selectedFriendIds = new HashSet<>();
    }

    @NonNull
    @Override
    public FriendSelectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_friend_selection, parent, false);
        return new FriendSelectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendSelectionViewHolder holder, int position) {
        UserViewModel userViewModel = friendViewModels.get(position);
        holder.bind(userViewModel, selectedFriendIds.contains(userViewModel.getUser().getId()));
    }

    @Override
    public int getItemCount() {
        return friendViewModels.size();
    }

    public Set<String> getSelectedFriendIds() {
        return new HashSet<>(selectedFriendIds);
    }

    public void clearSelection() {
        selectedFriendIds.clear();
        notifyDataSetChanged();
        if (listener != null) {
            listener.onSelectionChanged(selectedFriendIds);
        }
    }

    class FriendSelectionViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageViewAvatar;
        private TextView textViewUsername;
        private TextView textViewLevel;
        private CheckBox checkBoxSelect;

        public FriendSelectionViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewAvatar = itemView.findViewById(R.id.imageViewAvatar);
            textViewUsername = itemView.findViewById(R.id.textViewUsername);
            textViewLevel = itemView.findViewById(R.id.textViewLevel);
            checkBoxSelect = itemView.findViewById(R.id.checkBoxSelect);
        }

        public void bind(UserViewModel userViewModel, boolean isSelected) {
            textViewUsername.setText(userViewModel.getUsername());
            textViewLevel.setText("Level " + userViewModel.getLevel());
            checkBoxSelect.setChecked(isSelected);

            // Set avatar using UserViewModel's getAvatar() method
            setAvatarImage(userViewModel.getAvatar());

            checkBoxSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                String friendId = userViewModel.getUser().getId();
                if (isChecked) {
                    selectedFriendIds.add(friendId);
                } else {
                    selectedFriendIds.remove(friendId);
                }

                if (listener != null) {
                    listener.onSelectionChanged(selectedFriendIds);
                }
            });

            // Make the entire row clickable
            itemView.setOnClickListener(v -> {
                checkBoxSelect.setChecked(!checkBoxSelect.isChecked());
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
