package com.kulenina.questix.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.kulenina.questix.R;
import com.kulenina.questix.databinding.ItemUserBinding;
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
        ItemUserBinding binding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.getContext()),
            R.layout.item_user, parent, false
        );
        return new UserViewHolder(binding);
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

    public void updateUserList(List<UserViewModel> newUserViewModels) {
        this.userViewModels = newUserViewModels;
        notifyDataSetChanged();
    }

    public List<UserViewModel> getUserList() {
        return userViewModels;
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private ItemUserBinding binding;

        public UserViewHolder(@NonNull ItemUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(UserViewModel userViewModel, OnUserClickListener listener) {
            binding.setUserViewModel(userViewModel);
            binding.executePendingBindings();

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(userViewModel.getUser());
                }
            });
        }
    }
}
