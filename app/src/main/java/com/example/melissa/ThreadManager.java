package com.example.melissa;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;
import java.util.UUID;

public class ThreadManager {
    private SharedPreferences prefs;

    // 생성자에서 SharedPreferences 초기화
    public ThreadManager(Context context) {
        prefs = context.getSharedPreferences("ThreadPrefs", Context.MODE_PRIVATE);
    }

    public void createNewThreadId(String newThreadId) {
        long currentTime = Calendar.getInstance().getTimeInMillis();
        prefs.edit()
                .putString("threadId", newThreadId)
                .putLong("lastResetTime", currentTime) // 현재 시간 저장
                .apply();
    }

    // Thread ID 가져오기 또는 생성
    public String getOrCreateThreadId() {
        long currentTime = Calendar.getInstance().getTimeInMillis();
        long resetTime = getResetTime();
        long lastResetTime = prefs.getLong("lastResetTime", 0L);
        String threadId = prefs.getString("threadId", null);

        // 새벽 4:30 이후로 넘어갔는지 확인
        if (currentTime >= resetTime && (lastResetTime == 0L || lastResetTime < resetTime)) {
            clearThreadId(); // 초기화
            return null;
        }

        // 기존 threadId가 없으면 새로 생성
        if (threadId == null) {
            return createNewThreadId();
        }

        return threadId;
    }

    // 새로운 Thread ID 생성
    private String createNewThreadId() {
        String newThreadId = UUID.randomUUID().toString(); // 고유 ID 생성
        long currentTime = Calendar.getInstance().getTimeInMillis();
        prefs.edit()
                .putString("threadId", newThreadId)
                .putLong("lastResetTime", currentTime)
                .apply();
        return newThreadId;
    }

    // Thread ID 초기화
    public void clearThreadId() {
        prefs.edit()
                .remove("threadId") // threadId 제거
                .remove("lastResetTime") // 초기화 시간도 제거
                .apply();
    }

    // 오늘 새벽 4:30 시간 반환
    private long getResetTime() {
        Calendar resetTime = Calendar.getInstance();
        resetTime.set(Calendar.HOUR_OF_DAY, 4);
        resetTime.set(Calendar.MINUTE, 30);
        resetTime.set(Calendar.SECOND, 0);
        resetTime.set(Calendar.MILLISECOND, 0);
        return resetTime.getTimeInMillis();
    }

    public boolean isInitialMessageSent(String threadId) {
        return prefs.getBoolean("initialMessageSent_" + threadId, false);
    }

    public void setInitialMessageSent(String threadId) {
        prefs.edit()
                .putBoolean("initialMessageSent_" + threadId, true)
                .apply();
    }

}
