package com.kulenina.questix.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.kulenina.questix.R;
import com.kulenina.questix.databinding.ItemFriendSelectionBinding;
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
        ItemFriendSelectionBinding binding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.getContext()),
            R.layout.item_friend_selection, parent, false
        );
        return new FriendSelectionViewHolder(binding);
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
        private ItemFriendSelectionBinding binding;

        public FriendSelectionViewHolder(@NonNull ItemFriendSelectionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(UserViewModel userViewModel, boolean isSelected) {
            // Set data binding variables
            binding.setUserViewModel(userViewModel);
            binding.setIsSelected(isSelected);
            binding.executePendingBindings();

            // Set up checkbox listener
            binding.checkBoxSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
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
                binding.checkBoxSelect.setChecked(!binding.checkBoxSelect.isChecked());
            });
        }

    }
}
