package com.example.melissa;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Calendar;

public class FragmentCalendar extends Fragment {

    private int year;
    private int month;

    // newInstance 메서드를 통해 프래그먼트를 생성하고 값을 전달
    public static FragmentCalendar newInstance(int year, int month) {
        FragmentCalendar fragment = new FragmentCalendar();
        Bundle args = new Bundle();
        args.putInt("year", year);
        args.putInt("month", month);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            year = getArguments().getInt("year");
            month = getArguments().getInt("month");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        GridLayout daysGrid = view.findViewById(R.id.days_grid);

        // Calendar 설정
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // GridLayout 초기화
        daysGrid.removeAllViews();

        // 빈 셀 추가
        for (int i = 0; i < firstDayOfWeek; i++) {
            Space emptySpace = createEmptySpace();
            daysGrid.addView(emptySpace);
        }

        // 날짜 추가
        for (int day = 1; day <= daysInMonth; day++) {
            TextView dayView = createDayView(day);
            daysGrid.addView(dayView);
        }

        return view;
    }

    // 빈 공간을 위한 Space 생성 메서드
    private Space createEmptySpace() {
        Space emptySpace = new Space(getContext());

        GridLayout.LayoutParams emptyParams = new GridLayout.LayoutParams();
        emptyParams.width = 0;
        emptyParams.height = 0;
        emptyParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        emptyParams.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        emptySpace.setLayoutParams(emptyParams);

        return emptySpace;
    }

    // 날짜를 표시할 TextView 생성 메서드
    private TextView createDayView(int day) {
        TextView dayView = new TextView(getContext());
        dayView.setText(String.valueOf(day));
        dayView.setGravity(android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.TOP); // 상단 중앙 정렬
        dayView.setPadding(8, 16, 8, 8); // 위쪽 패딩을 줄여 최상단에 배치되도록 조정
        dayView.setTextSize(12);

        // 주말 스타일 설정 예시 (일요일: 붉은색, 토요일: 파란색)
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        if (dayOfWeek == Calendar.SUNDAY) {
            dayView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else if (dayOfWeek == Calendar.SATURDAY) {
            dayView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        }

        // 레이아웃 설정
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = 0;  // 세로 균등 분배를 위한 설정
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        dayView.setLayoutParams(params);

        return dayView;
    }
}
