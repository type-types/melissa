package com.example.melissa.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.melissa.R;
import com.example.melissa.activities.ViewDiaryActivity;
import com.example.melissa.database.DatabaseHelper;

import java.util.Calendar;
import java.util.Map;

public class FragmentCalendar extends Fragment {

    private int year;
    private int month;

    private Map<String, String> titlesMap;
    private DatabaseHelper databaseHelper;

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

        databaseHelper = new DatabaseHelper(requireContext());
        databaseHelper.openDatabase();
        titlesMap = databaseHelper.getTitlesForMonth(year, month);
        databaseHelper.closeDatabase();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Refresh the titles map
        databaseHelper.openDatabase();
        titlesMap = databaseHelper.getTitlesForMonth(year, month);
        databaseHelper.closeDatabase();

        // Redraw the calendar with the updated data
        refreshCalendar();
    }

    private void refreshCalendar() {
        View view = getView();
        if (view != null) {
            GridLayout daysGrid = view.findViewById(R.id.days_grid);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, 1);

            int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
            int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

            daysGrid.removeAllViews();

            // Add empty spaces for days before the first day of the month
            for (int i = 0; i < firstDayOfWeek; i++) {
                Space emptySpace = createEmptySpace();
                daysGrid.addView(emptySpace);
            }

            // Add day views for each day in the month
            for (int day = 1; day <= daysInMonth; day++) {
                TextView dayView = createDayView(day);
                daysGrid.addView(dayView);
            }
        }
    }

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

    private TextView createDayView(int day) {
        TextView dayView = new TextView(getContext());

        String monthStr = String.format("%02d", month + 1);
        String dayStr = String.format("%02d", day);
        String dateStr = year + "-" + monthStr + "-" + dayStr;

        dayView.setText(String.valueOf(day));
        dayView.setGravity(android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.TOP);
        dayView.setPadding(8, 16, 8, 8);
        dayView.setTextSize(12);

        if (titlesMap.containsKey(dateStr)) {
            String title = titlesMap.get(dateStr);

            if (title.length() > 10) {
                title = title.substring(0, 10) + "...";
            }

            String styledTitle = "\n\u2022 " + title;
            dayView.append(styledTitle);
            dayView.setTextSize(12);
            dayView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        if (dayOfWeek == Calendar.SUNDAY) {
            dayView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
        } else if (dayOfWeek == Calendar.SATURDAY) {
            dayView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark));
        }

        dayView.setOnClickListener(v -> showDayInfoDialog(day));

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = 0;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        dayView.setLayoutParams(params);

        return dayView;
    }

    private void showDayInfoDialog(int day) {
        String selectedDate = year + "-" + (month + 1) + "-" + day;

        Intent intent = new Intent(getContext(), ViewDiaryActivity.class);
        intent.putExtra("selectedDate", selectedDate);
        startActivity(intent);
    }
}
