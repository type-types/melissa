package com.example.melissa.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import com.example.melissa.R;
import com.example.melissa.database.SQLiteHelper;
import com.example.melissa.database.Summary;

import org.json.JSONObject;

public class ViewDiaryActivity extends AppCompatActivity {

    private static final String TAG = "ViewDiaryActivity";

    private TextView tvDate, tvTitle, tvContent;
    private Button btnViewConversation;
    private SQLiteHelper dbHelper;
    private String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_diary);

        // Layout에서 뷰 초기화
        tvDate = findViewById(R.id.tv_date);
        tvTitle = findViewById(R.id.tv_title);
        tvContent = findViewById(R.id.tv_content);
        btnViewConversation = findViewById(R.id.btn_view_conversation);

        dbHelper = new SQLiteHelper(this);

        // Intent로부터 선택된 날짜 수신
        selectedDate = getIntent().getStringExtra("selectedDate");

        if (selectedDate != null && !selectedDate.isEmpty()) {
            tvDate.setText(selectedDate); // 상단에 날짜 표시
            loadDiary(selectedDate); // 해당 날짜의 데이터 로드
        } else {
            Log.e(TAG, "선택된 날짜가 전달되지 않았습니다.");
            tvDate.setText("No Date Selected");
            tvTitle.setText("No Title");
            tvContent.setText("No Content");
            btnViewConversation.setEnabled(false); // 대화보기 버튼 비활성화
        }
    }

    /**
     * 특정 날짜의 데이터를 SQLiteHelper에서 불러와 화면에 표시하는 메서드.
     *
     * @param date 조회할 날짜
     */
    private void loadDiary(String date) {
        try {
            Summary summary = dbHelper.getSummaryByDate(date);

            if (summary != null) {
                // Summary 데이터 가져오기
                String summaryJson = summary.getSummaryJson();
                String conversationJson = summary.getConversationJson(); // 대화 JSON

                // JSON 파싱하여 title과 summary 추출
                JSONObject summaryJsonObject = new JSONObject(summaryJson);
                String title = summaryJsonObject.optString("title", "No Title");
                String htmlContent = summaryJsonObject.optString("summary", "No Content");

                // 데이터를 화면에 설정
                tvTitle.setText(title);
                tvContent.setText(HtmlCompat.fromHtml(htmlContent, HtmlCompat.FROM_HTML_MODE_LEGACY));

                // 대화 전체보기 버튼 클릭 이벤트
                btnViewConversation.setOnClickListener(v -> {
                    Intent intent = new Intent(ViewDiaryActivity.this, ConversationActivity.class);
                    intent.putExtra("conversationJson", conversationJson); // 대화 내용을 전달
                    startActivity(intent);
                });
            } else {
                // 데이터가 없는 경우 기본값 표시
                tvTitle.setText("No Title");
                tvContent.setText("No Content");
                btnViewConversation.setEnabled(false); // 대화보기 버튼 비활성화
                Log.d(TAG, "해당 날짜에 대한 데이터가 없습니다: " + date);
            }
        } catch (Exception e) {
            Log.e(TAG, "데이터 로드 중 오류 발생", e);
            tvTitle.setText("Error Loading Title");
            tvContent.setText("Error Loading Content");
            btnViewConversation.setEnabled(false); // 대화보기 버튼 비활성화
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // SQLiteHelper 닫기
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
