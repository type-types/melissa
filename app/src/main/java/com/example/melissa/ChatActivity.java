package com.example.melissa;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private RecyclerView recyclerView;
    private ChatApiManager chatApiManager;
    private Runnable pollingRunnable;

    private EditText inputMessage;
    private Button sendButton;

    private String threadId; // 생성된 Thread ID
    private String runId;    // 생성된 Run ID

    private final Handler handler = new Handler(Looper.getMainLooper()); // 메인 스레드 핸들러
    private static final long POLLING_INTERVAL = 1000L; // 폴링 간격 (1초)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerView = findViewById(R.id.recycler_view);
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        chatApiManager = new ChatApiManager();

        inputMessage = findViewById(R.id.input_message);
        sendButton = findViewById(R.id.send_button);

        sendButton.setOnClickListener(v -> sendMessage());

        initializeChat();
    }

    private void initializeChat() {
        List<ChatMessage> initialMessages = new ArrayList<>();
        initialMessages.add(new ChatMessage("assistant", "Hello! How can I assist you?"));

        chatApiManager.createThread(initialMessages, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                threadId = result;
                Log.d(TAG, "Thread 생성 성공: " + threadId);
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Thread 생성 실패: " + errorMessage);
            }
        });
    }

    private void sendMessage() {
        String content = inputMessage.getText().toString().trim();
        if (content.isEmpty()) {
            Log.w(TAG, "빈 메시지는 전송할 수 없습니다.");
            return;
        }

        addMessage(new ChatMessage("user", content));
        inputMessage.setText("");

        createRun(content);
    }

    private void createRun(String content) {
        chatApiManager.createRun(threadId, BuildConfig.ASSISTANT_ID, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runId = result;
                Log.d(TAG, "Run 생성 성공: " + runId);
                runPolling();
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Run 생성 실패: " + errorMessage);
            }
        });
    }

    private void runPolling() {
        this.pollingRunnable = () -> {
            chatApiManager.retrieveRun(threadId, runId, new ApiCallback<JsonObject>() {
                @Override
                public void onSuccess(JsonObject result) {
                    String status = result.get("status").getAsString();
                    Log.d(TAG, "Run 상태 확인: " + status);

                    if ("completed".equals(status)) {
                        fetchAssistantMessages();
                    } else {
                        handler.postDelayed(pollingRunnable, POLLING_INTERVAL);
                    }
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Run 상태 확인 실패: " + errorMessage);
                }
            });
        };

        handler.post(pollingRunnable); // 첫 번째 폴링 시작
    }

    private void fetchAssistantMessages() {
        chatApiManager.listMessages(threadId, new ApiCallback<List<ChatMessage>>() {
            @Override
            public void onSuccess(List<ChatMessage> messages) {
                chatMessages.clear(); // 기존 메시지 초기화
                chatMessages.addAll(messages); // 새 메시지 추가
                chatAdapter.notifyDataSetChanged(); // UI 갱신
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "메시지 가져오기 실패: " + errorMessage);
            }
        });
    }

    private void addMessage(ChatMessage message) {
        chatMessages.add(message);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        recyclerView.scrollToPosition(chatMessages.size() - 1);
    }
}
