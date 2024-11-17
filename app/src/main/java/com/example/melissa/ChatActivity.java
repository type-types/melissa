package com.example.melissa;

import android.content.Context;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private JSONArray userAnswers = new JSONArray(); // 사용자 답변 저장용
    private String fileName = "user_answers.json";   // 저장할 파일명

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
    DailyDataManager dataManager;

    public class DailySummary {

        private final String activities; // 필수
        private final String gratitude;  // 필수
        private final String plans;      // 필수

        public DailySummary(String activities, String gratitude, String plans) {
            if (activities == null || activities.isEmpty() ||
                    gratitude == null || gratitude.isEmpty() ||
                    plans == null || plans.isEmpty()) {
                throw new IllegalArgumentException("All fields are required and cannot be null or empty.");
            }
            this.activities = activities;
            this.gratitude = gratitude;
            this.plans = plans;
        }

        public String getActivities() {
            return activities;
        }

        public String getGratitude() {
            return gratitude;
        }

        public String getPlans() {
            return plans;
        }

        @Override
        public String toString() {
            return "DailySummary{" +
                    "activities='" + activities + '\'' +
                    ", gratitude='" + gratitude + '\'' +
                    ", plans='" + plans + '\'' +
                    '}';
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        scrollView = findViewById(R.id.scrollView);
        chatLayout = findViewById(R.id.chatLayout);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        //dataManager = new DailyDataManager(savedInstanceState);
        // 초기 질문 표시
        addBotMessage(questions[currentQuestionIndex]);

        btnSend.setOnClickListener(v -> {
            String userMessage = etMessage.getText().toString().trim();
            if (!userMessage.isEmpty()) {
                addUserMessage("나: " + userMessage);
                etMessage.setText("");

                // 현재 질문에 대한 응답만 처리
                if (currentQuestionIndex >= 0 && currentQuestionIndex < questions.length) {
                    // JSON에 사용자 답변 추가
                    saveAnswerToJson(userMessage);

                    // JSON 파일 저장
                    saveJsonToFile();

                    DailySummary summary = getDailySummaryFromUserAnswers();
                    if (summary != null) {
                        sendDailySummaryToServer(summary);
                    }

                    // 다음 질문으로 이동
                    currentQuestionIndex++;
                }

                // 다음 질문 출력 또는 종료 메시지
                if (currentQuestionIndex < questions.length) {
                    addBotMessage(questions[currentQuestionIndex]);
                } else {
                    addBotMessage("알려줘서 고마워! 이제 저장을 통해서 달력에 기록하자!");
                    btnSend.setEnabled(false); // 더 이상 질문이 없으므로 버튼 비활성화
                }
            }
        });
    }

    private void sendDailySummaryToServer(DailySummary summary) {
        new Thread(() -> {
            try {
                // Create URL
                URL url = new URL("http://183.98.149.222:15439/daily/");

                // Open connection
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                // Create JSON body
                JSONObject requestBody = new JSONObject();
                requestBody.put("activities", summary.getActivities());
                requestBody.put("gratitude", summary.getGratitude());
                requestBody.put("plans", summary.getPlans());

                // Send request
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = requestBody.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                // Read response
                int responseCode = connection.getResponseCode();
                InputStream stream = (responseCode == 200) ? connection.getInputStream() : connection.getErrorStream();

                try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    if (responseCode == 200) {
                        // Parse response JSON
                        JSONObject responseJson = new JSONObject(response.toString());

                        // JSON 데이터를 바로 파일로 저장
                        saveResToFile(responseJson);

                        // UI 업데이트
                        runOnUiThread(() -> {
                            addBotMessage("데이터가 성공적으로 저장되었어!");
                        });
                    } else {
                        // Handle server error response
                        Log.e("ServerError", "Response Code: " + responseCode + ", Message: " + response);
                        runOnUiThread(() -> {
                            addBotMessage("서버 응답에 문제가 있어! 다시 시도해줘.");
                        });
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    addBotMessage("데이터를 보내는 중 문제가 발생했어!");
                });
            }
        }).start();
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

    private void saveAnswerToJson(String answer) {
        try {
            // 인덱스 유효성 검사
            if (currentQuestionIndex >= 0 && currentQuestionIndex < questions.length) {
                JSONObject answerObject = new JSONObject();
                answerObject.put("question", questions[currentQuestionIndex]);
                answerObject.put("answer", answer);

                userAnswers.put(answerObject);
                addBotMessage("saveAnswerToJson 완료");
            } else {
                Log.e("ChatActivity", "Invalid index for questions array: " + currentQuestionIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
            addBotMessage("JSON 데이터 추가 중 오류가 발생했습니다.");
        }
    }

    private DailySummary getDailySummaryFromUserAnswers() {
        String activities = null;
        String gratitude = null;
        String plans = null;

        try {
            for (int i = 0; i < userAnswers.length(); i++) {
                JSONObject answerObject = userAnswers.getJSONObject(i);
                String question = answerObject.getString("question");
                String answer = answerObject.getString("answer");

                switch (question) {
                    case "오늘 뭐했어?":
                        activities = answer;
                        break;
                    case "감사한 일 3가지를 알려줘!":
                        gratitude = answer;
                        break;
                    case "내일 뭐 먹을거야?":
                        plans = answer;
                        break;
                    default:
                        // Skip unrelated questions
                        break;
                }
            }

            // Check if all required fields are present
            if (activities != null && gratitude != null && plans != null) {
                return new DailySummary(activities, gratitude, plans);
            } else {
                addBotMessage("아직 요약데이터 충분하지 않음");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null; // Return null if any required field is missing
    }



    private void saveJsonToFile() {
        try {
            // 현재 날짜 가져오기
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String currentDate = sdf.format(new Date());

            // Note 객체 생성 및 데이터 설정
            Note note = new Note();
            note.setTitle("사용자 답변"); // 제목 설정 (필요에 따라 변경 가능)
            note.setNote(userAnswers.toString()); // 사용자 답변(JSONArray)을 문자열로 변환하여 저장
            note.setDate(currentDate); // 현재 날짜 설정

            // Note 객체를 JSON 문자열로 변환
            JSONObject noteObject = new JSONObject();
            noteObject.put("title", note.getTitle());
            noteObject.put("note", note.getNote());
            noteObject.put("date", note.getDate());

            String jsonString = noteObject.toString();

            // 파일명 설정 (예: 2023-10-05.json)
            String filename = currentDate + ".json";

            // JSON 문자열을 파일에 저장 (try-with-resources 구문 사용)
            try (FileOutputStream outputStream = openFileOutput(filename, Context.MODE_PRIVATE)) {
                outputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
            }

            // 로그로 저장 성공 알림
            Log.d("ChatActivity", "파일 저장 경로: " + getFilesDir() + "/" + filename);
            Log.d("ChatActivity", "파일 내용: " + jsonString);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ChatActivity", "JSON 파일 저장 중 오류", e);

            // 오류 시에만 Bot 메시지로 알림
            String errorMessage = "JSON 파일 저장 중 오류가 발생했습니다.";
            addBotMessage(errorMessage);

            // Toast 메시지로 오류 알림
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }
    private void saveResToFile(JSONObject responseJson) {
        try {
            // Parse the response
            int id = responseJson.getInt("id");
            String createdAt = responseJson.getString("created_at");
            JSONObject summaryJson = responseJson.getJSONObject("summary");

            JSONArray todayActivities = summaryJson.getJSONArray("today_activities");
            JSONArray gratitudePoints = summaryJson.getJSONArray("gratitude_points");
            JSONArray plans = summaryJson.getJSONArray("plans");
            double satisfactionScore = summaryJson.getDouble("satisfaction_score");
            String satisfactionReason = summaryJson.getString("satisfaction_reason");

            // Create a new JSONObject for this entry
            JSONObject newEntry = new JSONObject();
            newEntry.put("id", id);
            newEntry.put("created_at", createdAt);

            JSONObject newSummary = new JSONObject();
            newSummary.put("today_activities", todayActivities);
            newSummary.put("gratitude_points", gratitudePoints);
            newSummary.put("plans", plans);
            newSummary.put("satisfaction_score", satisfactionScore);
            newSummary.put("satisfaction_reason", satisfactionReason);

            newEntry.put("summary", newSummary);

            // File name fixed as "MelissaRes.json"
            String filename = "MelissaRes.json";

            // Check if the file exists
            File file = new File(getFilesDir(), filename);
            if (!file.exists()) {
                try {
                    // Create an empty file
                    boolean isFileCreated = file.createNewFile();
                    if (isFileCreated) {
                        Log.d("ChatActivity", "새로운 파일이 생성되었습니다: " + file.getAbsolutePath());
                    }
                } catch (IOException e) {
                    Log.e("ChatActivity", "파일 생성 중 오류 발생: " + e.getMessage());
                }
            }

            // Read existing file contents, if available
            JSONArray existingEntries = new JSONArray();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(openFileInput(filename)))) {
                StringBuilder fileContent = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    fileContent.append(line);
                }
                existingEntries = new JSONArray(fileContent.toString());
            } catch (Exception e) {
                Log.d("ChatActivity", "기존 파일이 없거나 읽기 중 오류 발생: " + e.getMessage());
            }

            // Add the new entry to the existing data
            existingEntries.put(newEntry);

            // Save back to the file
            try (FileOutputStream outputStream = openFileOutput(filename, Context.MODE_PRIVATE)) {
                outputStream.write(existingEntries.toString().getBytes(StandardCharsets.UTF_8));
            }

            // Log for debugging
            Log.d("ChatActivity", "파일 저장 경로: " + file.getAbsolutePath());
            Log.d("ChatActivity", "저장된 파일 내용: " + existingEntries.toString());

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ChatActivity", "JSON 파일 저장 중 오류", e);

            // Notify the user and log the error
            String errorMessage = "JSON 파일 저장 중 오류가 발생했습니다.";
            addBotMessage(errorMessage);
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }

    }


}
