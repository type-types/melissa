package com.example.melissa;

import android.os.Looper;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;

/**
 * API 호출을 관리하는 클래스.
 */
public class ChatApiManager {
    private static final String TAG = ChatApiManager.class.getSimpleName(); // 로깅 태그
    private final GptApiService apiService; // Retrofit API 서비스
    private final android.os.Handler handler = new android.os.Handler(Looper.getMainLooper()); // 메인 쓰레드 핸들러
    private static final long POLLING_INTERVAL = 1000L; // 폴링 간격 (1초)

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
     * Run 생성 API 호출
     *
     * @param threadId     스레드 ID
     * @param assistantId  Assistant ID
     * @param callback     성공/실패 콜백
     */
    public void createRun(String threadId, String assistantId, ApiCallback<String> callback) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("assistant_id", assistantId);

        Call<JsonObject> call = apiService.createRun(
                "Bearer " + BuildConfig.OPENAI_API_KEY,
                "assistants=v2",
                threadId,
                requestBody
        );

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String runId = response.body().get("id").getAsString(); // Run ID 추출
                    callback.onSuccess(runId);
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
     * Run 상태를 주기적으로 확인하는 메서드
     *
     * @param threadId  스레드 ID
     * @param runId     Run ID
     * @param callback  성공/실패 콜백
     */
    public void runPolling(String threadId, String runId, ApiCallback<String> callback) {
        final Runnable pollingRunnable = new Runnable() {
            @Override
            public void run() {
                retrieveRun(threadId, runId, new ApiCallback<JsonObject>() {
                    @Override
                    public void onSuccess(JsonObject result) {
                        String status = result.get("status").getAsString();
                        Log.d(TAG, "Run 상태 확인: " + status);

                        if ("completed".equals(status)) {
                            callback.onSuccess("completed");
                        } else {
                            // 1초 후 다시 호출
                            handler.postDelayed(pollingRunnable, POLLING_INTERVAL);
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        callback.onFailure("Run 상태 확인 실패: " + errorMessage);
                    }
                });
            }
        };

        // 첫 번째 폴링 시작
        handler.post(pollingRunnable);
    }

    /**
     * Run의 상태를 가져오는 메서드
     *
     * @param threadId  스레드 ID
     * @param runId     Run ID
     * @param callback  성공/실패 콜백
     */
    private void retrieveRun(String threadId, String runId, ApiCallback<JsonObject> callback) {
        Call<JsonObject> call = apiService.retrieveRun(
                "Bearer " + BuildConfig.OPENAI_API_KEY, // Authorization 헤더
                "assistants=v2",                        // OpenAI-Beta 헤더
                threadId,                               // Path 파라미터: threadId
                runId                                   // Path 파라미터: runId
        );

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
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
     * 지정된 스레드에서 메시지 리스트를 가져옵니다.
     *
     * OpenAI API의 listMessages 엔드포인트를 호출하여 주어진 스레드 ID에 해당하는
     * 메시지 데이터를 서버로부터 가져옵니다. 요청 성공 시, 응답 데이터를
     * ChatMessage 리스트로 변환하여 반환합니다.
     *
     * @param threadId 메시지를 가져올 스레드의 ID
     * @param callback 성공 시 ChatMessage 리스트를 반환하는 콜백
     */
    public void listMessages(String threadId, ApiCallback<List<ChatMessage>> callback) {
        // API 호출
        Call<JsonObject> call = apiService.listMessages(
                "Bearer " + BuildConfig.OPENAI_API_KEY,
                "assistants=v2", // OpenAI-Beta 헤더 추가
                threadId
        );

        // 네트워크 요청 처리
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ChatMessage> messages = parseMessagesFromResponse(response.body());
                    callback.onSuccess(messages);
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
     * 서버 응답에서 메시지 리스트를 파싱합니다.
     *
     * @param response 서버로부터 받은 JSON 응답
     * @return ChatMessage 리스트
     */
    private List<ChatMessage> parseMessagesFromResponse(JsonObject response) {
        List<ChatMessage> messages = new ArrayList<>();
        JsonArray jsonMessages = response.getAsJsonArray("data");

        for (int i = 0; i < jsonMessages.size(); i++) {
            JsonObject messageObject = jsonMessages.get(i).getAsJsonObject();
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
