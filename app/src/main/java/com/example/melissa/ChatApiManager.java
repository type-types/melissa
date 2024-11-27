package com.example.melissa;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

/**
 * API 호출을 관리하는 클래스. ChatMessage 리스트를 JSON으로 변환하여 요청 처리.
 */
public class ChatApiManager {

    private final GptApiService apiService;

    public ChatApiManager() {
        this.apiService = RetrofitClient.getInstance().create(GptApiService.class);
    }

    /**
     * ChatMessage 리스트를 JsonArray로 변환하는 함수.
     *
     * @param chatMessages 변환할 ChatMessage 리스트
     * @return 변환된 JsonArray
     */
    private JsonArray convertChatMessagesToJson(List<ChatMessage> chatMessages) {
        JsonArray jsonArray = new JsonArray();

        for (ChatMessage chatMessage : chatMessages) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("role", chatMessage.getRole());
            jsonObject.addProperty("content", chatMessage.getContent());
            jsonArray.add(jsonObject);
        }

        return jsonArray;
    }

    /**
     * 새로운 스레드를 생성하는 API 호출.
     *
     * @param chatMessages ChatMessage 리스트
     * @param callback     성공/실패 콜백
     */
    public void createThread(List<ChatMessage> chatMessages, ApiCallback<String> callback) {
        JsonObject requestBody = constructThreadRequestBody(chatMessages);

        Call<JsonObject> call = apiService.createThread(
                "Bearer " + BuildConfig.OPENAI_API_KEY,
                "assistants=v2",
                requestBody
        );

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String threadId = response.body().get("id").getAsString();
                    callback.onSuccess(threadId);
                } else {
                    callback.onFailure("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    /**
     * 스레드 생성 요청 본문을 구성하는 메서드.
     *
     * @param chatMessages ChatMessage 리스트
     * @return 요청 본문 JsonObject
     */
    private JsonObject constructThreadRequestBody(List<ChatMessage> chatMessages) {
        JsonObject requestBody = new JsonObject();

        // ChatMessage 리스트를 JSON 배열로 변환
        JsonArray messagesArray = convertChatMessagesToJson(chatMessages);
        requestBody.add("messages", messagesArray);
        requestBody.add("tool_resources", null);
        requestBody.add("metadata", new JsonObject());

        return requestBody;
    }
}
