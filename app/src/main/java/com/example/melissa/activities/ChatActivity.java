package com.example.melissa.activities;

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

import com.example.melissa.network.ApiCallback;
import com.example.melissa.BuildConfig;
import com.example.melissa.adapters.ChatAdapter;
import com.example.melissa.network.ChatApiManager;
import com.example.melissa.models.ChatMessage;
import com.example.melissa.R;
import com.example.melissa.utils.ThreadManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import com.example.melissa.database.SQLiteHelper;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private RecyclerView recyclerView;
    private ChatApiManager chatApiManager;
    private Runnable pollingRunnable;
    private SQLiteHelper dbHelper; // SQLiteHelper 인스턴스

    private EditText inputMessage;
    private Button sendButton;

    private String runId; // 생성된 Run ID
    private JsonObject assistantResponseJson; // assistant 응답 저장 변수


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

        // Save 버튼에 클릭 리스너 추가
        saveButton.setOnClickListener(v -> {
            processSummaryRequest(); // 요약 요청 실행
        });

        // EditText에서 엔터키 입력 처리
        inputMessage.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                sendButton.performClick(); // 보내기 버튼 클릭 동작 실행
                return true;
            }
            return false;
        });

        // SQLiteHelper 초기화
        dbHelper = new SQLiteHelper(this);

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

    private void processSummaryRequest() {
        // 1. 요청 본문 생성
        JsonObject requestBody = new JsonObject();
        JsonArray messages = new JsonArray();

        // System 메시지 추가
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content",
                "당신은 사용자의 일일 활동, 감사한 일, 감정, 그리고 계획들을 깔끔하게 정리해주는 비서입니다.\n" +
                        "**Instruction**\n" +
                        "- 사용자의 입력을 분석하여 각 카테고리별로 핵심 포인트를 추출하여 정리해주세요.\n" +
                        "- 반드시 지정된 JSON 형식으로 출력해야 합니다.\n");
        messages.add(systemMessage);

        // User 메시지 추가
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");

        // chatMessages에서 user 대화 내용을 결합
        StringBuilder userContent = new StringBuilder();
        for (ChatMessage message : chatMessages) {
            if ("user".equals(message.getRole())) {
                userContent.append(message.getContent()).append("\n");
            }
        }
        userMessage.addProperty("content", userContent.toString().trim());
        messages.add(userMessage);

        requestBody.addProperty("model", "gpt-4");
        requestBody.add("messages", messages);

        // 2. GPT 요청
        chatApiManager.generalGptRequest(requestBody, new ApiCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject result) {
                // Assistant 응답에서 content 필드 추출
                String content = extractContentFromResponse(result);

                // chatMessages를 JSON 문자열로 변환
                Gson gson = new Gson();
                String fullConversationJson = gson.toJson(chatMessages);

                // 결과를 DB에 저장
                saveToDatabase(content, fullConversationJson);
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "요약 요청 실패: " + errorMessage);
            }
        });
    }

    /**
     * OpenAI 응답에서 content 필드만 추출.
     *
     * @param response OpenAI API 응답 JSON 객체
     * @return content 필드의 값
     */
    private String extractContentFromResponse(JsonObject response) {
        try {
            return response
                    .getAsJsonArray("choices")
                    .get(0)
                    .getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content")
                    .getAsString();
        } catch (Exception e) {
            Log.e(TAG, "응답에서 content를 추출하는 중 오류 발생", e);
            return null;
        }
    }

    /**
     * 요약 데이터와 전체 대화 데이터를 SQLite DB에 저장.
     *
     * @param summaryJson       요약 내용 (JSON 문자열)
     * @param fullConversationJson 전체 대화 내용 (JSON 문자열)
     */
    private void saveToDatabase(String summaryJson, String fullConversationJson) {
        if (dbHelper == null) {
            Log.e(TAG, "SQLiteHelper가 초기화되지 않았습니다!");
            Toast.makeText(this, "오류: 데이터베이스가 초기화되지 않았습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put("summary_json", summaryJson);
            values.put("full_conversation_json", fullConversationJson);

            // 데이터 삽입 전에 로그로 출력
            Log.d(TAG, "저장될 데이터: ");
            Log.d(TAG, "Summary JSON: " + summaryJson);
            Log.d(TAG, "Full Conversation JSON: " + fullConversationJson);

            long rowId = db.insert("summaries", null, values);
            if (rowId != -1) {
                Log.d(TAG, "데이터 저장 성공. Row ID: " + rowId);

                // 저장 완료 토스트 메시지 표시
                runOnUiThread(() -> Toast.makeText(this, "저장되었습니다!", Toast.LENGTH_SHORT).show());
            } else {
                Log.e(TAG, "데이터 저장 실패");
                runOnUiThread(() -> Toast.makeText(this, "저장에 실패했습니다.", Toast.LENGTH_SHORT).show());
            }
        } catch (Exception e) {
            Log.e(TAG, "데이터 저장 중 오류 발생", e);
            runOnUiThread(() -> Toast.makeText(this, "오류 발생: 저장할 수 없습니다.", Toast.LENGTH_SHORT).show());
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }
}
