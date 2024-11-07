package com.example.melissa;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ChatActivity extends AppCompatActivity {

    private ScrollView scrollView;
    private LinearLayout chatLayout;
    private EditText etMessage;
    private Button btnSend;

    private String[] questions = {
            "안녕 난 멜리사야!",
            "오늘 뭐했어?",
            "오늘 아쉬운 점이 뭐야?",
            "어떻게 하면 괜찮아질 것 같아?",
            "오늘 그 일에 대한 감정이 어땠어?",
            "감사한 일 3가지를 알려줘!",
            "내일 어떤 약속이 있어? 뭐할거야?",
            "내일 뭐 먹을거야?"
    };

    private int currentQuestionIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        scrollView = findViewById(R.id.scrollView);
        chatLayout = findViewById(R.id.chatLayout);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        // 초기 질문 표시
        addBotMessage(questions[currentQuestionIndex]);

        btnSend.setOnClickListener(v -> {
            String userMessage = etMessage.getText().toString().trim();
            if (!userMessage.isEmpty()) {
                addUserMessage("나: " + userMessage);
                etMessage.setText("");

                currentQuestionIndex++;
                if (currentQuestionIndex < questions.length) {
                    addBotMessage(questions[currentQuestionIndex]);
                } else {
                    addBotMessage("알려줘서 고마워! 이제 저장을 통해서 달력에 기록하자!");
                }
            }
        });
    }

    private void addUserMessage(String message) {
        TextView messageView = new TextView(this);
        messageView.setText(message);
        messageView.setBackgroundResource(R.drawable.right_bubble);
        messageView.setTextSize(16);
        messageView.setPadding(16, 8, 16, 8);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.END;
        params.setMargins(0, 8, 0, 8);

        messageView.setLayoutParams(params);
        chatLayout.addView(messageView);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN)); // 스크롤 최하단 이동
    }

    private void addBotMessage(String message) {
        TextView messageView = new TextView(this);
        messageView.setText("멜리사: " + message);
        messageView.setBackgroundResource(R.drawable.left_bubble);
        messageView.setTextSize(16);
        messageView.setPadding(16, 8, 16, 8);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.START;
        params.setMargins(0, 8, 0, 8);

        messageView.setLayoutParams(params);
        chatLayout.addView(messageView);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN)); // 스크롤 최하단 이동
    }
}
