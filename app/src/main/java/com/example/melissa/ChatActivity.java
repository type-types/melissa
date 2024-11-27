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

    private EditText inputMessage; // 사용자 입력란
    private Button sendButton;     // 보내기 버튼

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

        // 입력란 및 버튼 초기화
        inputMessage = findViewById(R.id.input_message);
        sendButton = findViewById(R.id.send_button);

        // "보내기" 버튼 클릭 이벤트
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        // 채팅방 초기화
        initializeChat();
    }

    /**
     * 새로운 스레드를 생성하고 초기 메시지를 전송합니다.
     */
    private void initializeChat() {
        String initialContent = "Welcome to the chat!";
        List<ChatMessage> initialMessages = new ArrayList<>();
        initialMessages.add(new ChatMessage("user", initialContent));

        chatApiManager.createThread(initialMessages, new ApiCallback<String>() {
            @Override
            public void onSuccess(String threadId) {
                Log.d(TAG, "스레드 생성 성공: " + threadId);
                addMessage(new ChatMessage("user", initialContent));
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "스레드 생성 실패: " + errorMessage);
            }
        });
    }

    /**
     * 사용자가 입력한 메시지를 전송합니다.
     */
    private void sendMessage() {
        String content = inputMessage.getText().toString().trim();

        if (content.isEmpty()) {
            Log.w(TAG, "빈 메시지는 전송할 수 없습니다.");
            return;
        }

        // 입력 메시지를 UI에 추가
        addMessage(new ChatMessage("user", content));

        // 입력 필드 비우기
        inputMessage.setText("");

        // 이후 서버와의 메시지 전송 로직 추가 가능 (예: API 호출)
        // chatApiManager.createMessage(threadId, content, new ApiCallback<>() { ... });
    }

    /**
     * 로컬 메시지 리스트에 새 메시지를 추가하고 화면을 갱신합니다.
     *
     * @param message 추가할 메시지 객체
     */
    private void addMessage(ChatMessage message) {
        chatMessages.add(message);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        recyclerView.scrollToPosition(chatMessages.size() - 1);
    }
}
