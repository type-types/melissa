package com.example.melissa;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ViewDiaryActivity extends AppCompatActivity {

    private TextView tvDate;
    private EditText etTitle, etContent;
    private Button btnSave, btnEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_diary);

        tvDate = findViewById(R.id.tv_date);
        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);
        btnSave = findViewById(R.id.btn_save);
        btnEdit = findViewById(R.id.btn_edit);

        // Intent로부터 선택된 날짜 수신
        String selectedDate = getIntent().getStringExtra("selectedDate");
        tvDate.setText(selectedDate);  // 상단에 날짜 표시

        // 로컬 데이터 조회하여 제목과 본문 불러오기 (없을 경우 빈 상태 유지)
        loadDiary(selectedDate);

        // 저장 버튼 클릭 시
        btnSave.setOnClickListener(v -> saveDiary(selectedDate));

        // 수정 버튼 클릭 시 (기존 내용 수정 가능)
        btnEdit.setOnClickListener(v -> enableEditing());
    }

    private void loadDiary(String date) {
        // SharedPreferences 등을 통해 데이터 로드
        // (여기서 title, content가 로드되었다고 가정)
        String title = ""; // 저장된 제목
        String content = ""; // 저장된 내용

        // 제목과 본문 내용이 있다면 화면에 표시
        etTitle.setText(title);
        etContent.setText(content);
    }

    private void saveDiary(String date) {
        // SharedPreferences 등을 통해 데이터를 저장
        // 저장 후, 조회 모드로 전환
        disableEditing();
    }

    private void enableEditing() {
        etTitle.setEnabled(true);
        etContent.setEnabled(true);
        btnSave.setVisibility(View.VISIBLE);
    }

    private void disableEditing() {
        etTitle.setEnabled(false);
        etContent.setEnabled(false);
        btnSave.setVisibility(View.GONE);
    }
}
