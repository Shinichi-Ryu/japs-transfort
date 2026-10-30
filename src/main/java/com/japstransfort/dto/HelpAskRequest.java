package com.japstransfort.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Request payload for POST /help/ask.
 */
public class HelpAskRequest {

    private String message;
    private List<ChatMessage> history = new ArrayList<>();

    public HelpAskRequest() {
    }

    public HelpAskRequest(String message, List<ChatMessage> history) {
        this.message = message;
        this.history = history != null ? history : new ArrayList<>();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ChatMessage> getHistory() {
        return history;
    }

    public void setHistory(List<ChatMessage> history) {
        this.history = history != null ? history : new ArrayList<>();
    }

    public static class ChatMessage {
        private String role;
        private String text;

        public ChatMessage() {
        }

        public ChatMessage(String role, String text) {
            this.role = role;
            this.text = text;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
