package com.adventure.solo.model.firebase;

import com.google.firebase.database.Exclude; // For excluding fields from Firebase
import com.google.firebase.database.ServerValue; // For server-side timestamp

public class ChatMessage {
    public String messageId; // Will be set from Firebase push key
    public String teamId;
    public String senderId;
    public String senderName; // Display name of the sender
    public String messageText;
    public Object timestamp; // Use Object for ServerValue.TIMESTAMP, will be Long after fetch

    public ChatMessage() {
        // Default constructor required for calls to DataSnapshot.getValue(ChatMessage.class)
    }

    public ChatMessage(String teamId, String senderId, String senderName, String messageText) {
        this.teamId = teamId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.messageText = messageText;
        this.timestamp = ServerValue.TIMESTAMP; // Firebase server sets the timestamp
    }

    @Exclude // Exclude this from Firebase as it's populated from timestamp Object after fetch
    public long getTimestampLong() {
        if (timestamp instanceof Long) {
            return (Long) timestamp;
        }
        return 0; // Or handle error/default, e.g., return System.currentTimeMillis() or throw exception
    }

    // Optional: Setter for timestamp if needed, though usually set by Firebase
    public void setTimestamp(Object timestamp) {
        this.timestamp = timestamp;
    }

    // Other getters and setters can be added if direct field access is not preferred.
    // For Firebase POJOs, public fields are common.
}
