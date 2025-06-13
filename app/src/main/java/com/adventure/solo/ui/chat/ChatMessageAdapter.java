package com.adventure.solo.ui.chat;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DiffUtil; // Import DiffUtil
import com.adventure.solo.R;
import com.adventure.solo.model.firebase.ChatMessage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    private List<ChatMessage> messages = new ArrayList<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault()); // More informative timestamp
    private String currentUserId;

    public ChatMessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
        if (currentUserId == null || currentUserId.isEmpty()) {
            // Fallback or log warning, as adapter might not behave correctly without a valid currentUserId
            android.util.Log.w("ChatMessageAdapter", "currentUserId is null or empty in constructor. Self-messages might not be styled correctly.");
            this.currentUserId = "UNKNOWN_USER_ID_ADAPTER_FALLBACK"; // Avoid NPE, but self-styling will be off
        }
    }

    // Method to update the list using DiffUtil for better performance
    public void submitList(List<ChatMessage> newMessages) {
        final ChatMessageDiffCallback diffCallback = new ChatMessageDiffCallback(this.messages, newMessages);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.messages.clear();
        this.messages.addAll(newMessages);
        diffResult.dispatchUpdatesTo(this);
    }

    // Keep addMessage for real-time additions if not using full list submission always
    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void clearMessages() {
        int size = messages.size();
        messages.clear();
        notifyItemRangeRemoved(0, size);
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view, sdf);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.bind(message, currentUserId);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView senderNameTextView, messageTextView, timestampTextView;
        LinearLayout messageBubbleLayout; // The inner layout that will be aligned
        SimpleDateFormat sdfRef;

        public MessageViewHolder(@NonNull View itemView, SimpleDateFormat sdf) {
            super(itemView);
            messageBubbleLayout = itemView.findViewById(R.id.message_bubble_layout);
            senderNameTextView = itemView.findViewById(R.id.sender_name_text_view);
            messageTextView = itemView.findViewById(R.id.message_text_text_view);
            timestampTextView = itemView.findViewById(R.id.timestamp_text_view);
            this.sdfRef = sdf;
        }

        public void bind(ChatMessage message, String currentUserId) {
            boolean isSelf = message.senderId != null && message.senderId.equals(currentUserId);

            LinearLayout.LayoutParams bubbleLayoutParams = (LinearLayout.LayoutParams) messageBubbleLayout.getLayoutParams();

            if (isSelf) {
                senderNameTextView.setVisibility(View.GONE); // Hide sender name for self
                messageTextView.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.chat_bubble_self));
                bubbleLayoutParams.gravity = Gravity.END;
                // Align timestamp to the end within the bubble (which itself is end-aligned)
                ((LinearLayout.LayoutParams) timestampTextView.getLayoutParams()).gravity = Gravity.END;
            } else {
                senderNameTextView.setVisibility(View.VISIBLE);
                senderNameTextView.setText(message.senderName != null ? message.senderName : "Unknown");
                messageTextView.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.chat_bubble_other));
                bubbleLayoutParams.gravity = Gravity.START;
                // Align timestamp to the start (or end, depending on style) for other messages
                ((LinearLayout.LayoutParams) timestampTextView.getLayoutParams()).gravity = Gravity.START; // Or keep END if preferred for all
            }
            messageBubbleLayout.setLayoutParams(bubbleLayoutParams);

            messageTextView.setText(message.messageText);

            if (message.timestamp instanceof Long) {
                timestampTextView.setText(sdfRef.format(new Date((Long) message.timestamp)));
            } else {
                timestampTextView.setText("sending...");
            }
        }
    }

    // DiffUtil Callback for more efficient RecyclerView updates
    public static class ChatMessageDiffCallback extends DiffUtil.Callback {
        private final List<ChatMessage> oldList;
        private final List<ChatMessage> newList;

        public ChatMessageDiffCallback(List<ChatMessage> oldList, List<ChatMessage> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() { return oldList.size(); }

        @Override
        public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            // Assuming messageId is unique and stable
            return oldList.get(oldItemPosition).messageId.equals(newList.get(newItemPosition).messageId);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            ChatMessage oldMsg = oldList.get(oldItemPosition);
            ChatMessage newMsg = newList.get(newItemPosition);
            // Compare relevant fields. For chat, usually text and timestamp might change if editable, but often they don't.
            return oldMsg.messageText.equals(newMsg.messageText) &&
                   (oldMsg.timestamp == null ? newMsg.timestamp == null : oldMsg.timestamp.equals(newMsg.timestamp));
        }
    }
}
