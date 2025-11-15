package com.emsi.emsipresence;

public class ChatMessage {
    private String content;
    private boolean isUser;
    private long timestamp;
    private String messageId; // Optional: for message identification

    public ChatMessage(String content, boolean isUser) {
        this.content = content;
        this.isUser = isUser;
        this.timestamp = System.currentTimeMillis();
        this.messageId = generateMessageId();
    }

    public ChatMessage(String content, boolean isUser, long timestamp) {
        this.content = content;
        this.isUser = isUser;
        this.timestamp = timestamp;
        this.messageId = generateMessageId();
    }

    private String generateMessageId() {
        return "msg_" + System.currentTimeMillis() + "_" + hashCode();
    }

    // Getters
    public String getContent() {
        return content;
    }

    public boolean isUser() {
        return isUser;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    // Setters (if needed)
    public void setContent(String content) {
        this.content = content;
    }

    // Utility methods
    public boolean isEmpty() {
        return content == null || content.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "content='" + content + '\'' +
                ", isUser=" + isUser +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ChatMessage that = (ChatMessage) obj;
        return messageId != null ? messageId.equals(that.messageId) : that.messageId == null;
    }

    @Override
    public int hashCode() {
        return messageId != null ? messageId.hashCode() : 0;
    }
}