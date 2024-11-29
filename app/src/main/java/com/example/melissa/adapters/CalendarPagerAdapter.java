package com.example.melissa.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.melissa.fragments.FragmentCalendar;

import java.util.Calendar;

public class CalendarPagerAdapter extends FragmentStateAdapter {

    public static final int START_POSITION = Integer.MAX_VALUE / 2; // 중간에서 시작
    private final int initialYear;
    private final int initialMonth;

    public CalendarPagerAdapter(@NonNull FragmentActivity fa, int initialYear, int initialMonth) {
        super(fa);
        this.initialYear = initialYear;
        this.initialMonth = initialMonth;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        int offset = position - START_POSITION;

        Calendar calendar = Calendar.getInstance();
        calendar.set(initialYear, initialMonth, 1);
        calendar.add(Calendar.MONTH, offset);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);

        return FragmentCalendar.newInstance(year, month);
    }

    @Override
    public int getItemCount() {
        return Integer.MAX_VALUE; // 무한 스와이프를 위한 대략적인 큰 숫자
    }
}
