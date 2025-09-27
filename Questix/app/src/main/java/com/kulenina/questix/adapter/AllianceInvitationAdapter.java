package com.kulenina.questix.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.kulenina.questix.R;
import com.kulenina.questix.databinding.ItemAllianceInvitationBinding;
import com.kulenina.questix.model.AllianceInvitation;

import java.util.ArrayList;
import java.util.List;

public class AllianceInvitationAdapter extends RecyclerView.Adapter<AllianceInvitationAdapter.InvitationViewHolder> {
    
    private List<AllianceInvitation> invitations;
    private OnInvitationActionListener listener;

    public interface OnInvitationActionListener {
        void onAcceptInvitation(AllianceInvitation invitation);
        void onDeclineInvitation(AllianceInvitation invitation);
    }

    public AllianceInvitationAdapter(OnInvitationActionListener listener) {
        this.invitations = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public InvitationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAllianceInvitationBinding binding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.getContext()),
            R.layout.item_alliance_invitation,
            parent,
            false
        );
        return new InvitationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull InvitationViewHolder holder, int position) {
        AllianceInvitation invitation = invitations.get(position);
        holder.binding.setInvitation(invitation);
        holder.binding.setListener(listener);
        holder.binding.executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return invitations.size();
    }

    public void updateInvitations(List<AllianceInvitation> newInvitations) {
        this.invitations.clear();
        if (newInvitations != null) {
            this.invitations.addAll(newInvitations);
        }
        notifyDataSetChanged();
    }

    public void removeInvitation(AllianceInvitation invitation) {
        int position = invitations.indexOf(invitation);
        if (position != -1) {
            invitations.remove(position);
            notifyItemRemoved(position);
        }
    }

    public static class InvitationViewHolder extends RecyclerView.ViewHolder {
        public ItemAllianceInvitationBinding binding;

        public InvitationViewHolder(@NonNull ItemAllianceInvitationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
