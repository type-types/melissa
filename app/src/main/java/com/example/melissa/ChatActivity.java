package com.example.melissa;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private RecyclerView recyclerView;
    private ChatApiManager chatApiManager;

    private EditText inputMessage;
    private Button sendButton;

    private String threadId; // 생성된 Thread ID
    private String runId;    // 생성된 Run ID
    private List<String> receivedMessageIds = new ArrayList<>(); // 중복 메시지 방지용 ID 저장소

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // RecyclerView 초기화
        recyclerView = findViewById(R.id.recycler_view);
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        // ChatApiManager 초기화
        chatApiManager = new ChatApiManager();

        // 입력 필드 및 버튼 초기화
        inputMessage = findViewById(R.id.input_message);
        sendButton = findViewById(R.id.send_button);

        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String content = inputMessage.getText().toString().trim();

        if (content.isEmpty()) {
            Log.w(TAG, "빈 메시지는 전송할 수 없습니다.");
            return;
        }

        // UI에 메시지 추가
        addMessage(new ChatMessage("user", content));
        inputMessage.setText("");

        // Run 생성 및 연속 작업 수행
        createRun(content);
    }

    private void createRun(String content) {
        chatApiManager.createRun(threadId, BuildConfig.ASSISTANT_ID, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runId = result; // Run ID 저장
                Log.d(TAG, "Run 생성 성공: " + runId);

                // Run 상태 확인
                runPolling();
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Run 생성 실패: " + errorMessage);
            }
        });
    }

    private void runPolling() {
        chatApiManager.runPolling(threadId, runId, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                Log.d(TAG, "Run 상태: " + result);

                // Run 완료 후 메시지 가져오기
                fetchAssistantMessages();
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Run 상태 확인 실패: " + errorMessage);
            }
        });
    }

    private void fetchAssistantMessages() {
        chatApiManager.listMessages(threadId, new ApiCallback<List<ChatMessage>>() {
            @Override
            public void onSuccess(List<ChatMessage> messages) {
                for (ChatMessage message : messages) {
                    // 새로운 메시지만 추가
                    if (!receivedMessageIds.contains(message.getId())) {
                        addMessage(message);
                        receivedMessageIds.add(message.getId()); // 메시지 ID 저장
                    }
                }
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
