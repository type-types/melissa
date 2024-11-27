package com.example.melissa;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import ai.picovoice.porcupine.*;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private TextView tvYear, tvMonth;
    private ViewPager2 viewPager;
    private CalendarPagerAdapter pagerAdapter;
    private int currentYear, currentMonth;

    private static final int START_POSITION = Integer.MAX_VALUE / 2;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private FloatingActionButtonHelper fabHelper;
    private PorcupineManager porcupineManager;
    private static final String ACCESS_KEY = BuildConfig.PICOVOICE_ACCESS_KEY; // Picovoice Access Key

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestMicrophonePermission(); // 마이크 권한 요청

        tvYear = findViewById(R.id.tv_year);
        tvMonth = findViewById(R.id.tv_month);
        viewPager = findViewById(R.id.viewPager);

        Calendar calendar = Calendar.getInstance();
        currentYear = calendar.get(Calendar.YEAR);
        currentMonth = calendar.get(Calendar.MONTH);

        pagerAdapter = new CalendarPagerAdapter(this, currentYear, currentMonth);
        viewPager.setAdapter(pagerAdapter);

        viewPager.setCurrentItem(START_POSITION, false);
        updateDateDisplay(currentYear, currentMonth);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                int offset = position - START_POSITION;
                Calendar selectedCalendar = Calendar.getInstance();
                selectedCalendar.set(currentYear, currentMonth, 1);
                selectedCalendar.add(Calendar.MONTH, offset);

                int year = selectedCalendar.get(Calendar.YEAR);
                int month = selectedCalendar.get(Calendar.MONTH);

                updateDateDisplay(year, month);
            }
        });

        View.OnClickListener dateClickListener = v -> showYearMonthPickerDialog();
        tvYear.setOnClickListener(dateClickListener);
        tvMonth.setOnClickListener(dateClickListener);

        FrameLayout mainLayout = findViewById(android.R.id.content);
        fabHelper = new FloatingActionButtonHelper(this);
        mainLayout.addView(fabHelper.getFab());

        // PorcupineManager 초기화
        try {
            porcupineManager = new PorcupineManager.Builder()
                    .setAccessKey(ACCESS_KEY)
                    .setKeywordPaths(new String[]{"melissa_ko_android_v3_0_0.ppn"})
                    .setModelPath("porcupine_params_ko.pv") // 한국어 모델 파일 경로 설정
                    .build(this, wakeWordCallback);
        } catch (PorcupineException e) {
            e.printStackTrace();
            Log.e("MainActivity", "PorcupineManager 초기화 실패: " + e.getMessage());
        }
    }

    private void requestMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "마이크 권한이 허용되었습니다.");
            } else {
                Log.d("MainActivity", "마이크 권한이 거부되었습니다.");
            }
        }
    }

    private void updateDateDisplay(int year, int month) {
        tvYear.setText(String.valueOf(year));
        tvMonth.setText(getMonthName(month));
    }

    private String getMonthName(int month) {
        String[] monthNames = {"January", "February", "March", "April", "May", "June", "July",
                "August", "September", "October", "November", "December"};
        return monthNames[month];
    }

    private void showYearMonthPickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Year and Month");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_year_month_picker, null);
        builder.setView(dialogView);

        NumberPicker yearPicker = dialogView.findViewById(R.id.year_picker);
        NumberPicker monthPicker = dialogView.findViewById(R.id.month_picker);

        int displayedYear = Integer.parseInt(tvYear.getText().toString());
        String displayedMonthName = tvMonth.getText().toString();
        int displayedMonth = getMonthIndex(displayedMonthName);

        yearPicker.setMinValue(2000);
        yearPicker.setMaxValue(2050);
        yearPicker.setValue(displayedYear);

        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(new String[]{"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"});
        monthPicker.setValue(displayedMonth);

        builder.setPositiveButton("OK", (dialog, which) -> {
            int selectedYear = yearPicker.getValue();
            int selectedMonth = monthPicker.getValue();

            int yearDifference = selectedYear - currentYear;
            int monthDifference = selectedMonth - currentMonth;
            int totalOffset = yearDifference * 12 + monthDifference;
            int newPosition = START_POSITION + totalOffset;

            viewPager.setCurrentItem(newPosition, false);
            updateDateDisplay(selectedYear, selectedMonth);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private int getMonthIndex(String monthName) {
        String[] monthNames = {"January", "February", "March", "April", "May", "June", "July",
                "August", "September", "October", "November", "December"};
        for (int i = 0; i < monthNames.length; i++) {
            if (monthNames[i].equalsIgnoreCase(monthName)) {
                return i;
            }
        }
        return 0;
    }

    // 웨이크 워드가 감지되었을 때 + 버튼 클릭 효과를 위한 콜백 설정
    private PorcupineManagerCallback wakeWordCallback = new PorcupineManagerCallback() {
        @Override
        public void invoke(int keywordIndex) {
            if (keywordIndex == 0) {
                Log.d("PorcupineWakeWord", "웨이크 워드 '멜리사' 감지됨"); // 로그 출력
                runOnUiThread(() -> fabHelper.getFab().performClick()); // + 버튼 효과 실행
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if (porcupineManager != null) {
            try {
                porcupineManager.start();
            } catch (PorcupineException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (porcupineManager != null) {
            try {
                porcupineManager.stop();
            } catch (PorcupineException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (porcupineManager != null) {
            porcupineManager.delete();
        }
    }
}
