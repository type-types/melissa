package com.example.melissa.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.melissa.R;

import org.json.JSONArray;
import org.json.JSONObject;

public class ConversationActivity extends AppCompatActivity {

    private LinearLayout conversationLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        // Layout에서 대화 표시 영역 초기화
        conversationLayout = findViewById(R.id.conversation_layout);

        // Intent로부터 대화 JSON 데이터 수신
        String conversationJson = getIntent().getStringExtra("conversationJson");
        if (conversationJson != null) {
            displayConversation(conversationJson);
        } else {
            addMessageView("No Conversation Available", "unknown");
        }
    }

    /**
     * 대화 데이터를 파싱하여 화면에 동적으로 추가
     *
     * @param conversationJson JSON 형식의 대화 데이터
     */
    private void displayConversation(String conversationJson) {
        try {
            JSONArray conversationArray = new JSONArray(conversationJson);

            // JSON 배열을 반복하며 메시지 추가
            for (int i = 0; i < conversationArray.length(); i++) {
                JSONObject message = conversationArray.getJSONObject(i);
                String role = message.optString("role", "unknown");
                String content = message.optString("content", "");

                // 메시지 추가
                addMessageView(content, role);
            }
        } catch (Exception e) {
            e.printStackTrace();
            addMessageView("Error loading conversation.", "unknown");
        }
    }

    /**
     * 메시지 내용을 표시할 뷰를 동적으로 추가
     *
     * @param message 대화 내용
     * @param role    메시지의 역할 ("user" 또는 "assistant")
     */
    private void addMessageView(String message, String role) {
        LayoutInflater inflater = LayoutInflater.from(this);

        if ("user".equals(role)) {
            // 사용자 메시지 추가
            View userMessageView = inflater.inflate(R.layout.item_message_user, conversationLayout, false);
            TextView tvUserMessage = userMessageView.findViewById(R.id.user_message_text);
            tvUserMessage.setText(message);
            conversationLayout.addView(userMessageView);
        } else if ("assistant".equals(role)) {
            // 시스템 메시지 추가
            View assistantMessageView = inflater.inflate(R.layout.item_message_assistant, conversationLayout, false);
            TextView tvAssistantMessage = assistantMessageView.findViewById(R.id.assistant_message_text);
            tvAssistantMessage.setText(message);
            conversationLayout.addView(assistantMessageView);
        } else {
            // 알 수 없는 역할 처리
            TextView unknownMessageView = new TextView(this);
            unknownMessageView.setText(message);
            unknownMessageView.setPadding(16, 8, 16, 8);
            conversationLayout.addView(unknownMessageView);
        }
    }
}
