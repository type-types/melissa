package com.example.melissa;

import java.util.List;

public class GPTRequest {
    private String model;
    private List<Message> messages;

    public GPTRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }

    public static class Message {
        private String role;  // "system", "user", "assistant" 중 하나
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        // **Getter for role**
        public String getRole() {
            return role;
        }

        // **Getter for content**
        public String getContent() {
            return content;
        }
    }
}
