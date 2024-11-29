package com.example.melissa;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
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

    private String runId; // 생성된 Run ID

    private final Handler handler = new Handler(Looper.getMainLooper()); // 메인 스레드 핸들러
    private static final long POLLING_INTERVAL = 1000L; // 폴링 간격 (1초)

    private ThreadManager threadManager; // ThreadManager 인스턴스

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
        threadManager = new ThreadManager(this); // ThreadManager 초기화

        inputMessage = findViewById(R.id.input_message);
        sendButton = findViewById(R.id.send_button);
        Button resetButton = findViewById(R.id.reset_button); // 초기화 버튼
        Button saveButton = findViewById(R.id.save_button); // 저장 버튼

        sendButton.setOnClickListener(v -> sendMessage());
        resetButton.setOnClickListener(v -> resetThreadId()); // 초기화 버튼 동작 설정

        // EditText에서 엔터키 입력 처리
        inputMessage.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                sendButton.performClick(); // 보내기 버튼 클릭 동작 실행
                return true;
            }
            return false;
        });

        initializeChat(); // 대화 초기화 및 이전 메시지 불러오기
    }

    private void initializeChat() {
        String threadId = threadManager.getOrCreateThreadId();

        if (threadId == null) {
            // threadId가 없으면 새로 생성
            chatApiManager.createThread(new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    Log.d(TAG, "Thread 생성 성공: " + result);

                    // ThreadManager에 새 threadId 저장
                    threadManager.createNewThreadId(result);

                    // 새 threadId로 대화 초기화
                    sendInitialMessage(result);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Thread 생성 실패: " + errorMessage);
                }
            });
        } else {
            // 기존 threadId가 있으면 이전 메시지 불러오기
            Log.d(TAG, "기존 Thread ID 사용: " + threadId);
            fetchAssistantMessages(threadId); // 이전 대화 내역 불러오기

            // 초기 메시지 전송 시도
            sendInitialMessage(threadId);
        }
    }

    private void sendInitialMessage(String threadId) {
        // 초기 메시지 전송 여부 확인
        if (threadManager.isInitialMessageSent(threadId)) {
            Log.d(TAG, "초기 메시지가 이미 전송되었습니다.");
            return;
        }

        String initialMessageContent = "오늘 뭐했어?";
        ChatMessage initialMessage = new ChatMessage("assistant", initialMessageContent); // 역할을 "assistant"로 설정
        addMessage(initialMessage);

        chatApiManager.createMessage(threadId, initialMessage, new ApiCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject result) {
                Log.d(TAG, "초기 메시지 전송 성공: " + result.toString());

                // 초기 메시지 전송 상태 저장
                threadManager.setInitialMessageSent(threadId);
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

        String threadId = threadManager.getOrCreateThreadId();
        if (threadId != null) {
            chatApiManager.createMessage(threadId, userMessage, new ApiCallback<JsonObject>() {
                @Override
                public void onSuccess(JsonObject result) {
                    Log.d(TAG, "메시지 전송 성공: " + result.toString());
                    createRun(threadId); // 메시지 전송 성공 후 Run 생성
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

    private void createRun(String threadId) {
        chatApiManager.createRun(threadId, BuildConfig.ASSISTANT_ID, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runId = result; // Run ID 저장
                Log.d(TAG, "Run 생성 성공: " + runId);

                if (runId != null) {
                    runPolling(threadId); // Run ID가 유효하면 Polling 시작
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

    private void runPolling(String threadId) {
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
                        fetchAssistantMessages(threadId);
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

    private void fetchAssistantMessages(String threadId) {
        chatApiManager.listMessages(threadId, new ApiCallback<List<ChatMessage>>() {
            @Override
            public void onSuccess(List<ChatMessage> messages) {
                if (messages != null && !messages.isEmpty()) {
                    Log.d(TAG, "메시지 가져오기 성공: " + messages.size() + "개의 메시지");
                    chatMessages.clear(); // 기존 메시지 초기화
                    chatMessages.addAll(messages); // 이전 메시지 추가
                    chatAdapter.notifyDataSetChanged(); // UI 갱신
                    recyclerView.scrollToPosition(chatMessages.size() - 1); // 마지막 메시지로 스크롤
                } else {
                    Log.d(TAG, "메시지 내역이 없습니다.");
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

    private void resetThreadId() {
        // Thread ID 초기화
        threadManager.clearThreadId();
        Log.d(TAG, "Thread ID가 초기화되었습니다.");

        // UI 초기화
        chatMessages.clear(); // 메시지 목록 초기화
        chatAdapter.notifyDataSetChanged(); // RecyclerView 갱신

        // 새로운 채팅방처럼 초기화
        initializeChat(); // 새 threadId로 재설정
    }

}
