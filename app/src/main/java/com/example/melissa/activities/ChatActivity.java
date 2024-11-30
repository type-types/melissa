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

import com.example.melissa.database.Summary;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
    private boolean isSummaryProcessing = false;

    private EditText inputMessage;
    private Button sendButton;
    private Button saveButton;

    private String runId; // 생성된 Run ID
    private String threadId; // 생성된 Thread ID
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
        saveButton = findViewById(R.id.save_button); // 저장 버튼

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
        threadManager.getOrCreateThreadId(new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                threadId = result; // Thread ID 저장
                Log.d(TAG, "Thread ID 초기화 완료: " + threadId);

                // 1. 이전 메시지 불러오기
                fetchAssistantMessages(threadId);

                // 2. 초기 메시지 전송
                sendInitialMessage(threadId);
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Thread ID 생성 실패: " + errorMessage);
                Toast.makeText(ChatActivity.this, "Thread ID 생성 실패: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
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

        // 사용자 메시지 객체 생성 및 UI에 추가
        ChatMessage userMessage = new ChatMessage("user", content);
        addMessage(userMessage);
        inputMessage.setText("");

        // threadId를 사용하여 openAI 서버에 메시지 전송
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
        if (isSummaryProcessing) {
            Log.w(TAG, "요약 요청이 이미 처리 중입니다.");
            Toast.makeText(this, "이미 요약 요청이 처리 중입니다.", Toast.LENGTH_SHORT).show();
            return; // 처리 중이면 함수 종료
        }

        // 요약 요청 상태로 변경
        isSummaryProcessing = true;
        saveButton.setEnabled(false); // 버튼 비활성화

        // 1. 요청 본문 생성
        JsonObject requestBody = new JsonObject();
        JsonArray messages = new JsonArray();

        // System 메시지 추가
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content",
                "당신은 사용자의 하루를 요약하고 정리하여 JSON 형식으로 응답하는 비서입니다.\n" +
                        "\n" +
                        "**Instruction**\n" +
                        "1. `title` 필드:\n" +
                        "   - 제목을 한국어로만 작성합니다. 하루를 돌아보았을 때 기분 좋거나 의미 있는 이름을 간결하고 창의적으로 작성하세요.\n" +
                        "\n" +
                        "2. `summary` 필드:\n" +
                        "   - 제목 및 내용을 한국어로만 작성합니다. HTML 형식으로 작성하며, 다음 정보를 포함하세요:\n" +
                        "     - 활동1, 활동2 등과 같이 주요 활동을 각각 나열하고, 해당 내용과 감정을 포함.\n" +
                        "     - 만족도를 0~10 사이로 평가하며, 이유를 간단히 설명.\n" +
                        "     - 미래 계획은 2개 이상의 주요 계획을 포함하고, 필요에 따라 더 많은 활동을 추가할 수 있도록 구성.\n" +
                        "     - 중요한 내용은 `<strong>` 태그를 사용하여 강조할 수 있습니다.\n" +
                        "\n" +
                        "3. 각 섹션은 `<h2>`와 `<h3>` 태그로 구분하여 작성하세요:\n" +
                        "   - `<h2>`는 주요 섹션(예: \"오늘의 주요 활동\", \"만족도\", \"내일의 계획\")의 제목을 작성.\n" +
                        "   - `<h3>`는 각 활동이나 세부 내용을 작성하며, 필요에 따라 더 많은 활동을 추가할 수 있도록 구성.\n" +
                        "   - 예시:\n" +
                        "     <html><body>\n" +
                        "     <h2>오늘의 주요 활동</h2>\n" +
                        "     <h3>활동 1</h3>\n" +
                        "     <p><strong>내용 1:</strong> 중요한 내용을 강조할 수 있습니다.</p>\n" +
                        "     <h3>활동 2</h3>\n" +
                        "     <p>내용 2</p>\n" +
                        "     ... (여기에 더 많은 활동을 추가할 수 있음.)\n" +
                        "     <h2>만족도</h2>\n" +
                        "     <p>n/10 - <strong>만족도 이유</strong>: 필요시 강조</p>\n" +
                        "     <h2>내일의 계획</h2>\n" +
                        "     <h3>계획 1</h3>\n" +
                        "     <p>계획 내용 1</p>\n" +
                        "     <h3>계획 2</h3>\n" +
                        "     <p><strong>계획 내용 2</strong>: 중요한 계획을 강조</p>\n" +
                        "     ... (여기에 더 많은 계획을 추가할 수 있음.)\n" +
                        "     </body></html>\n" +
                        "\n" +
                        "4. 출력은 반드시 아래의 JSON 형식이어야 합니다:\n" +
                        "{\n" +
                        "    \"title\": \"하루를 대표하는 제목\",\n" +
                        "    \"summary\": \"<html><body>...</body></html>\"\n" +
                        "}"
        );
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

        // 요청 본문에 모델과 메시지 배열 추가
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

                // 현재 날짜 가져오기
                String currentDate = getCurrentDate();

                // 결과를 DB에 저장
                if (content != null && fullConversationJson != null) {
                    saveToDatabase(currentDate, content, fullConversationJson);
                } else {
                    Log.e(TAG, "content 또는 fullConversationJson이 null입니다.");
                }
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
    private void saveToDatabase(String date, String summaryJson, String fullConversationJson) {
        if (dbHelper == null) {
            Log.e(TAG, "SQLiteHelper가 초기화되지 않았습니다!");
            Toast.makeText(this, "오류: 데이터베이스가 초기화되지 않았습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Summary 객체 생성
            Summary summary = new Summary(date, summaryJson, fullConversationJson);

            // SQLiteHelper의 upsertSummary 메서드 호출
            dbHelper.upsertSummary(summary);

            // 저장 완료 토스트 메시지 표시
            runOnUiThread(() -> Toast.makeText(this, "데이터가 저장되었습니다!", Toast.LENGTH_SHORT).show());

            isSummaryProcessing = false; // 요청 처리 완료
            saveButton.setEnabled(true); // 버튼 활성화

            Log.d(TAG, "데이터 저장 성공: 날짜 = " + date);

            int rowCount = dbHelper.getRowCount();
            Log.d("ChatActivity", "chat_summaries.db 파일 내 총 행 개수: " + rowCount);
        } catch (Exception e) {
            Log.e(TAG, "데이터 저장 중 오류 발생", e);
            runOnUiThread(() -> Toast.makeText(this, "저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show());
        }
    }


    // 현재 날짜를 가져오는 메서드 추가
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

}
