package com.emsi.emsipresence;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private static final int TYPE_USER_MESSAGE = 0;
    private static final int TYPE_BOT_MESSAGE = 1;

    private final List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = (viewType == TYPE_USER_MESSAGE) ?
                R.layout.item_user_message : R.layout.item_bot_message;

        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser() ? TYPE_USER_MESSAGE : TYPE_BOT_MESSAGE;
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;
        private final TextView timeText; // Optional: if you want to show timestamps

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message);
            timeText = itemView.findViewById(R.id.text_time); // Optional
        }

        public void bind(ChatMessage message) {
            messageText.setText(message.getContent());

            // Optional: Show timestamp if timeText view exists
            if (timeText != null && message.getTimestamp() > 0) {
                String time = DateFormat.format("HH:mm", message.getTimestamp()).toString();
                timeText.setText(time);
                timeText.setVisibility(View.VISIBLE);
            } else if (timeText != null) {
                timeText.setVisibility(View.GONE);
            }
        }
    }
}