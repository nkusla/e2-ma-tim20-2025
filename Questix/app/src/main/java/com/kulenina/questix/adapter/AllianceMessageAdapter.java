package com.kulenina.questix.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kulenina.questix.R;
import com.kulenina.questix.model.AllianceMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AllianceMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER_MESSAGE = 1;
    private static final int VIEW_TYPE_OTHER_MESSAGE = 2;

    private List<AllianceMessage> messages;
    private String currentUserId;
    private SimpleDateFormat timeFormat;

    public AllianceMessageAdapter(String currentUserId) {
        this.messages = new ArrayList<>();
        this.currentUserId = currentUserId;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @Override
    public int getItemViewType(int position) {
        AllianceMessage message = messages.get(position);
        if (message.senderId.equals(currentUserId)) {
            return VIEW_TYPE_USER_MESSAGE;
        } else {
            return VIEW_TYPE_OTHER_MESSAGE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_USER_MESSAGE) {
            View view = inflater.inflate(R.layout.item_message_user, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_other, parent, false);
            return new OtherMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AllianceMessage message = messages.get(position);

        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).bind(message);
        } else if (holder instanceof OtherMessageViewHolder) {
            ((OtherMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void updateMessages(List<AllianceMessage> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }

    public void addMessage(AllianceMessage message) {
        this.messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    class UserMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView textMessage;
        private TextView textTimestamp;

        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_view_message);
            textTimestamp = itemView.findViewById(R.id.text_view_timestamp);
        }

        public void bind(AllianceMessage message) {
            textMessage.setText(message.message);
            textTimestamp.setText(timeFormat.format(new Date(message.timestamp)));
        }
    }

    class OtherMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView textUsername;
        private TextView textMessage;
        private TextView textTimestamp;

        public OtherMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textUsername = itemView.findViewById(R.id.text_view_username);
            textMessage = itemView.findViewById(R.id.text_view_message);
            textTimestamp = itemView.findViewById(R.id.text_view_timestamp);
        }

        public void bind(AllianceMessage message) {
            textUsername.setText(message.senderUsername);
            textMessage.setText(message.message);
            textTimestamp.setText(timeFormat.format(new Date(message.timestamp)));
        }
    }
}
