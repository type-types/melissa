package com.example.melissa;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";

    private GptApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Retrofit 인스턴스에서 GptApiService 생성
        apiService = RetrofitClient.getInstance().create(GptApiService.class);

        // 채팅 방 입장 시 스레드 생성
        createThread();
    }

    private void createThread() {
        // Request Body 생성
        JsonObject requestBody = new JsonObject();
        requestBody.add("messages", new JsonObject());
        requestBody.add("tool_resources", null);
        requestBody.add("metadata", new JsonObject());

        // API 호출
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
                    Log.d(TAG, "Thread created successfully: " + threadId);

                    // 이후 메시지 전송 등의 작업 수행
                    createMessage(threadId, "Hello!");
                } else {
                    Log.e(TAG, "Failed to create thread: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Error creating thread", t);
            }
        });
    }

    private void createMessage(String threadId, String content) {
        // Request Body 생성
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("content", content);

        // API 호출
        Call<JsonObject> call = apiService.createMessage(
                "Bearer " + BuildConfig.OPENAI_API_KEY,
                "assistants=v2",
                threadId,
                requestBody
        );

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String messageId = response.body().get("id").getAsString();
                    Log.d(TAG, "Message sent successfully: " + messageId);
                } else {
                    Log.e(TAG, "Failed to send message: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Error sending message", t);
            }
        });
    }
}
