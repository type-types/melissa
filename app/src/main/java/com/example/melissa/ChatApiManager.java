package com.example.melissa;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI API 호출을 관리하는 클래스.
 */
public class ChatApiManager {
    private static final String TAG = ChatApiManager.class.getSimpleName(); // 로깅 태그
    private final GptApiService apiService; // Retrofit API 서비스

    public ChatApiManager() {
        this.apiService = RetrofitClient.getInstance().create(GptApiService.class);
    }

    /**
     * 새로운 스레드를 생성하는 API 호출 (빈 메시지로 생성).
     *
     * @param callback 성공/실패 콜백
     */
    public void createThread(ApiCallback<String> callback) {
        JsonObject requestBody = new JsonObject();
        requestBody.add("messages", new JsonArray()); // 빈 메시지 배열
        requestBody.add("tool_resources", null);
        requestBody.add("metadata", new JsonObject());

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
     * 메시지 전송 API 호출
     *
     * @param threadId   메시지를 보낼 스레드 ID
     * @param message    전송할 ChatMessage 객체
     * @param callback   성공/실패 콜백
     */
    /**
     * 메시지 전송 API 호출
     *
     * @param threadId   메시지를 보낼 스레드 ID
     * @param message    전송할 ChatMessage 객체
     * @param callback   성공/실패 콜백
     */
    public void createMessage(String threadId, ChatMessage message, ApiCallback<JsonObject> callback) {
        // 요청 본문 생성
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("role", message.getRole()); // role 필드 추가
        requestBody.addProperty("content", message.getContent()); // content 필드 추가

        Call<JsonObject> call = apiService.createMessage(
                "Bearer " + BuildConfig.OPENAI_API_KEY, // Authorization 헤더
                "assistants=v2",                        // OpenAI-Beta 헤더
                threadId,                               // Path 파라미터: threadId
                requestBody                             // 요청 본문
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
     * Run 생성 API 호출
     *
     * @param threadId     스레드 ID
     * @param assistantId  Assistant ID
     * @param callback     성공/실패 콜백
     */
    public void createRun(String threadId, String assistantId, ApiCallback<String> callback) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("assistant_id", assistantId);

        // 로그 추가: 요청 정보 확인
        Log.d(TAG, "Run 생성 요청: Thread ID = " + threadId + ", Assistant ID = " + assistantId);

        Call<JsonObject> call = apiService.createRun(
                "Bearer " + BuildConfig.OPENAI_API_KEY,
                "assistants=v2",
                threadId,
                requestBody
        );

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                // 응답 성공 여부 확인
                if (response.isSuccessful() && response.body() != null) {
                    String runId = response.body().get("id").getAsString(); // Run ID 추출
                    Log.d(TAG, "Run 생성 성공: Run ID = " + runId);
                    callback.onSuccess(runId);
                } else {
                    // 실패 로그 추가: 상태 코드 및 응답 본문
                    String errorBody = response.errorBody() != null ? response.errorBody().toString() : "null";
                    Log.e(TAG, "Run 생성 실패: 상태 코드 = " + response.code() + ", 응답 본문 = " + errorBody);
                    callback.onFailure("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                // 네트워크 오류 로그 추가
                Log.e(TAG, "Run 생성 네트워크 오류: " + t.getMessage(), t);
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    /**
     * Run의 상태를 확인하는 API 호출
     *
     * @param threadId  스레드 ID
     * @param runId     Run ID
     * @param callback  성공/실패 콜백
     */
    public void retrieveRun(String threadId, String runId, ApiCallback<JsonObject> callback) {
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
     * 지정된 스레드에서 메시지 리스트를 가져오는 API 호출.
     *
     * @param threadId 메시지를 가져올 스레드의 ID
     * @param callback 성공 시 ChatMessage 리스트를 반환하는 콜백
     */
    public void listMessages(String threadId, ApiCallback<List<ChatMessage>> callback) {
        Call<JsonObject> call = apiService.listMessages(
                "Bearer " + BuildConfig.OPENAI_API_KEY,
                "assistants=v2", // OpenAI-Beta 헤더 추가
                threadId
        );

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ChatMessage> messages = ChatMessage.fromJsonResponse(response.body());
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
}
