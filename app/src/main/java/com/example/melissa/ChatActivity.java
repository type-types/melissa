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

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private ScrollView scrollView;
    private LinearLayout chatLayout;
    private EditText etMessage;
    private Button btnSend;

    private GPTApiService apiService;
    private List<GPTRequest.Message> messages; // 대화 상태 유지
    private int currentQuestionIndex = 0; // 현재 핵심 질문 인덱스
    private int followUpQuestionCount = 0; // 현재 꼬리 질문 개수
    private final String[] coreQuestions = {
            "오늘 한 일이 뭐야?",
            "오늘 좋았던 점과 아쉬웠던 점이 뭔지 알려줄래?",
            "오늘 한 일에 대해서 감정이 어땠는지 궁금해.",
            "오늘 감사한 점 3가지가 뭔지 말해줘.",
            "내일 뭐할 계획이야?"
    };
    private final int maxFollowUpQuestions = 3; // 꼬리 질문 최대 개수
    private boolean isWaitingForUserInput = false; // 유저 입력 대기 상태

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        scrollView = findViewById(R.id.scrollView);
        chatLayout = findViewById(R.id.chatLayout);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        apiService = RetrofitClient.create(); // Retrofit 클라이언트 생성
        messages = new ArrayList<>(); // 대화 기록 리스트 초기화

        // 시스템 초기 메시지 추가
        messages.add(new GPTRequest.Message("system",
                "이 GPT는 따뜻하고 친근한 친구처럼, 사용자의 심리적 고민과 문제를 경청하고 공감하며 적절한 조언을 제공합니다. 사용자의 감정을 최우선으로 고려하며, 비판하지 않고 지지적인 태도로 대응합니다. 근거 기반 심리학 원칙에 따라 신중하고 세심한 조언을 제공하되, 대면 상담의 대체 역할은 하지 않습니다. 사용자가 자신의 활동이나 경험을 말했을 때, 세부적인 정보를 자연스럽게 물어보며 대화를 이어갑니다. 모든 대화는 반말로 친근하게 진행하되, 과도한 아는 척은 하지 않습니다. 또한, 대화 중 emoji나 이모티콘은 사용하지 않습니다. 반응은 명확하고 간결하면서도 사용자의 감정에 민감하게 대응합니다."
        ));

        // 첫 번째 질문 출력
        fetchGeneratedQuestion(coreQuestions[currentQuestionIndex]);

        btnSend.setOnClickListener(v -> {
            if (isWaitingForUserInput) {
                String userMessage = etMessage.getText().toString().trim();
                if (!userMessage.isEmpty()) {
                    addUserMessage("나: " + userMessage);
                    etMessage.setText("");

                    // 사용자 메시지를 대화 기록에 추가
                    messages.add(new GPTRequest.Message("user", userMessage));

                    // 유저 입력을 처리 후 다음 단계로 이동
                    handleAssistantResponse();
                }
            }
        });
    }

    private void fetchGeneratedQuestion(String question) {
        isWaitingForUserInput = false; // 질문이 생성 중일 때 유저 입력 비활성화
        List<GPTRequest.Message> tempMessages = new ArrayList<>();
        tempMessages.add(new GPTRequest.Message("system",
                question + "위의 질문을 바탕으로 사용자에게 반말로 친근하게 질문을 리프레이징해서 물음표로 끝나는 질문 만들기"
        ));

        GPTRequest request = new GPTRequest("gpt-3.5-turbo", tempMessages);

        apiService.getGPTResponse(request).enqueue(new Callback<GPTResponse>() {
            @Override
            public void onResponse(Call<GPTResponse> call, Response<GPTResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String generatedQuestion = response.body().getChoices().get(0).getMessage().getContent();

                    // GPT의 질문을 대화에 추가
                    addBotMessage(generatedQuestion);
                    messages.add(new GPTRequest.Message("assistant", generatedQuestion));

                    // 유저 입력 대기 상태로 설정
                    isWaitingForUserInput = true;
                } else {
                    addBotMessage("질문 생성 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<GPTResponse> call, Throwable t) {
                addBotMessage("오류 발생: " + t.getMessage());
            }
        });
    }

    private void handleAssistantResponse() {
        isWaitingForUserInput = false; // 유저 입력 대기 상태 해제

        if (followUpQuestionCount < maxFollowUpQuestions) {
            // 꼬리 질문 생성
            followUpQuestionCount++;
            fetchGeneratedFollowUpQuestion();
        } else {
            // 꼬리 질문 완료 후 다음 핵심 질문으로 이동
            followUpQuestionCount = 0;
            currentQuestionIndex++;
            if (currentQuestionIndex < coreQuestions.length) {
                fetchGeneratedQuestion(coreQuestions[currentQuestionIndex]);
            } else {
                addBotMessage("대화가 종료되었습니다. 오늘 하루를 되돌아보는 데 도움 되었길 바랍니다!");
            }
        }
    }

    private void fetchGeneratedFollowUpQuestion() {
        // 꼬리 질문 생성 요청
        List<GPTRequest.Message> tempMessages = new ArrayList<>(messages); // 현재 메시지 기록 복사
        tempMessages.add(new GPTRequest.Message("system",
                "유저의 이전 답변을 기반으로 자연스럽고 간결한 꼬리 질문을 하나 생성해주세요."));

        GPTRequest request = new GPTRequest("gpt-3.5-turbo", tempMessages);

        apiService.getGPTResponse(request).enqueue(new Callback<GPTResponse>() {
            @Override
            public void onResponse(Call<GPTResponse> call, Response<GPTResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String followUpQuestion = response.body().getChoices().get(0).getMessage().getContent();

                    // 꼬리 질문을 대화에 추가
                    addBotMessage(followUpQuestion);
                    messages.add(new GPTRequest.Message("assistant", followUpQuestion));

                    // 유저 입력 대기 상태로 설정
                    isWaitingForUserInput = true;
                } else {
                    addBotMessage("꼬리 질문 생성 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<GPTResponse> call, Throwable t) {
                addBotMessage("오류 발생: " + t.getMessage());
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
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
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
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
}
