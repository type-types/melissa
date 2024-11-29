package com.example.melissa.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.melissa.models.ChatMessage;
import com.example.melissa.R;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_ASSISTANT = 1;

    private List<ChatMessage> chatMessages;

    public ChatAdapter(List<ChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = chatMessages.get(position);
        return message.getRole().equals("user") ? VIEW_TYPE_USER : VIEW_TYPE_ASSISTANT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_user, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_assistant, parent, false);
            return new AssistantMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);
        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).bind(message);
        } else if (holder instanceof AssistantMessageViewHolder) {
            ((AssistantMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView textView;

        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.user_message_text);
        }

        public void bind(ChatMessage message) {
            textView.setText(message.getContent());
        }
    }

    static class AssistantMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView textView;

        public AssistantMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.assistant_message_text);
        }

        public void bind(ChatMessage message) {
            textView.setText(message.getContent());
        }
    }
}
