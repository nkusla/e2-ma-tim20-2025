package com.kulenina.questix.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kulenina.questix.databinding.ItemMemberProgressBinding;
import com.kulenina.questix.model.MissionProgress;
import com.kulenina.questix.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemberProgressAdapter extends RecyclerView.Adapter<MemberProgressAdapter.MemberProgressViewHolder> {

    private List<MissionProgress> memberProgressList = new ArrayList<>();
    private Map<String, User> memberMap = new HashMap<>();
    private String currentUserId;

    public void setData(List<MissionProgress> progressList, List<User> members, String currentUserId) {
        System.out.println("DEBUG: MemberProgressAdapter.setData() called with " + 
                (progressList != null ? progressList.size() : 0) + " progress items and " + 
                (members != null ? members.size() : 0) + " members");
        
        this.memberProgressList = progressList != null ? progressList : new ArrayList<>();
        this.currentUserId = currentUserId;
        
        // Create a map for quick user lookup
        this.memberMap.clear();
        if (members != null) {
            for (User member : members) {
                memberMap.put(member.getId(), member);
                System.out.println("DEBUG: Added member to map: " + member.username + " (ID: " + member.getId() + ")");
            }
        }
        
        System.out.println("DEBUG: Notifying adapter of data change, item count: " + getItemCount());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MemberProgressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMemberProgressBinding binding = ItemMemberProgressBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new MemberProgressViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberProgressViewHolder holder, int position) {
        MissionProgress progress = memberProgressList.get(position);
        User member = memberMap.get(progress.userId);
        
        String memberName = member != null ? member.username : "Unknown Member";
        boolean isCurrentUser = currentUserId != null && currentUserId.equals(progress.userId);
        
        holder.bind(progress, memberName, isCurrentUser);
    }

    @Override
    public int getItemCount() {
        return memberProgressList.size();
    }
    
    public List<MissionProgress> getAllProgress() {
        return new ArrayList<>(memberProgressList);
    }

    static class MemberProgressViewHolder extends RecyclerView.ViewHolder {
        private final ItemMemberProgressBinding binding;

        public MemberProgressViewHolder(@NonNull ItemMemberProgressBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(MissionProgress progress, String memberName, boolean isCurrentUser) {
            binding.setMemberProgress(progress);
            binding.setMemberName(memberName);
            binding.setIsCurrentUser(isCurrentUser);
            binding.executePendingBindings();
        }
    }
}
