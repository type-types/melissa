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
        chatApiManager.createThread(new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                threadId = result; // 생성된 threadId 저장
                Log.d(TAG, "Thread 생성 성공: " + threadId);

                // 첫 메시지 전송
                sendInitialMessage();
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Thread 생성 실패: " + errorMessage);
            }
        });
    }

    private void sendInitialMessage() {
        String initialMessageContent = "오늘 뭐했어?";
        ChatMessage initialMessage = new ChatMessage("assistant", initialMessageContent); // 역할을 "assistant"로 설정
        addMessage(initialMessage);

        chatApiManager.createMessage(threadId, initialMessage, new ApiCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject result) {
                Log.d(TAG, "초기 메시지 전송 성공: " + result.toString());
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "초기 메시지 전송 실패: " + errorMessage);
            }
        });
    }

    private void sendMessage() {
        String content = inputMessage.getText().toString().trim();
        if (content.isEmpty()) {
            Log.w(TAG, "빈 메시지는 전송할 수 없습니다.");
            return;
        }

        ChatMessage userMessage = new ChatMessage("user", content);
        addMessage(userMessage);
        inputMessage.setText("");

        if (threadId != null) {
            // 메시지 생성
            chatApiManager.createMessage(threadId, userMessage, new ApiCallback<JsonObject>() {
                @Override
                public void onSuccess(JsonObject result) {
                    Log.d(TAG, "메시지 전송 성공: " + result.toString());
                    createRun(); // 메시지 전송 성공 후 Run 생성
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "메시지 전송 실패: " + errorMessage);
                }
            });
        } else {
            Log.e(TAG, "threadId가 초기화되지 않았습니다. 메시지를 전송할 수 없습니다.");
        }
    }

    private void createRun() {
        chatApiManager.createRun(threadId, BuildConfig.ASSISTANT_ID, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runId = result; // Run ID 저장
                Log.d(TAG, "Run 생성 성공: " + runId);

                // Run 생성 성공 후 runPolling 호출
                if (runId != null) {
                    runPolling(); // Run ID가 유효하면 Polling 시작
                } else {
                    Log.e(TAG, "Run ID가 null입니다. Polling을 시작할 수 없습니다.");
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Run 생성 실패: " + errorMessage);
            }
        });
    }

    private void runPolling() {
        if (runId == null) {
            Log.e(TAG, "Run ID가 null입니다. Polling을 시작할 수 없습니다.");
            return;
        }

        pollingRunnable = () -> {
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

        handler.post(pollingRunnable); // 첫 번째 Polling 시작
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
