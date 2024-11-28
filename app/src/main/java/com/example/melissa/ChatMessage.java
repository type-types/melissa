package com.example.melissa;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ChatMessage {
    private String role;
    private String content;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    /**
     * ChatMessage 리스트를 JSON 배열로 변환하는 메서드.
     *
     * @param messages 변환할 ChatMessage 리스트
     * @return 변환된 JsonArray
     */
    public static JsonArray toJsonArray(List<ChatMessage> messages) {
        JsonArray jsonArray = new JsonArray();
        for (ChatMessage message : messages) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("role", message.getRole());
            jsonObject.addProperty("content", message.getContent());
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }

    /**
     * JSON 응답에서 ChatMessage 리스트로 변환하는 메서드.
     *
     * @param jsonResponse 서버에서 받은 JSON 응답 객체
     * @return 변환된 ChatMessage 리스트
     */
    public static List<ChatMessage> fromJsonResponse(JsonObject jsonResponse) {
        List<ChatMessage> messages = new ArrayList<>();
        JsonArray data = jsonResponse.getAsJsonArray("data");

        for (int i = 0; i < data.size(); i++) {
            JsonObject messageObject = data.get(i).getAsJsonObject();
            String role = messageObject.get("role").getAsString();

            JsonArray contentArray = messageObject.getAsJsonArray("content");
            for (int j = 0; j < contentArray.size(); j++) {
                JsonObject contentObject = contentArray.get(j).getAsJsonObject();
                if ("text".equals(contentObject.get("type").getAsString())) {
                    String content = contentObject.getAsJsonObject("text").get("value").getAsString();
                    messages.add(new ChatMessage(role, content));
                }
            }
        }

        return messages;
    }
}
