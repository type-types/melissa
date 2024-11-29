package com.example.melissa.models;

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
     * ChatMessage 객체를 JsonObject로 변환하는 메서드.
     *
     * 이 메서드는 단일 ChatMessage 객체를 JSON 형식으로 변환하여 반환합니다.
     * 주로 API 호출 시 단일 메시지를 요청 본문에 포함해야 할 때 사용됩니다.
     *
     * @param message 변환할 ChatMessage 객체
     * @return 변환된 JsonObject
     */
    public static JsonObject toJson(ChatMessage message) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("role", message.getRole()); // role 필드 추가
        jsonObject.addProperty("content", message.getContent()); // content 필드 추가
        return jsonObject;
    }

    /**
     * ChatMessage 리스트를 JSON 배열로 변환하는 메서드.
     *
     * 이 메서드는 ChatMessage 객체 리스트를 순회하며 각 객체를 JSON 형식으로 변환한 뒤,
     * JsonArray 객체에 추가합니다. 이를 통해 API 호출에 필요한 요청 본문 형태를
     * 쉽게 구성할 수 있습니다.
     *
     * @param messages 변환할 ChatMessage 리스트
     * @return 변환된 JsonArray (각 ChatMessage 객체가 JSON 형식으로 포함됨)
     */
    public static JsonArray toJsonArray(List<ChatMessage> messages) {
        JsonArray jsonArray = new JsonArray();
        for (ChatMessage message : messages) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("role", message.getRole()); // role 필드 추가
            jsonObject.addProperty("content", message.getContent()); // content 필드 추가
            jsonArray.add(jsonObject); // JsonObject를 JsonArray에 추가
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
