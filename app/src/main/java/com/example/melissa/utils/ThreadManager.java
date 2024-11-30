package com.example.melissa.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.melissa.network.ApiCallback;
import com.example.melissa.network.ChatApiManager;

import java.util.Calendar;

public class ThreadManager {
    private static final String TAG = "ThreadManager";
    private SharedPreferences prefs;
    private ChatApiManager chatApiManager;

    // 생성자에서 SharedPreferences 초기화
    public ThreadManager(Context context) {
        prefs = context.getSharedPreferences("ThreadPrefs", Context.MODE_PRIVATE);
        chatApiManager = new ChatApiManager();
    }

    /**
     * 저장된 Thread ID 반환
     *
     * @return 저장된 Thread ID (없으면 null)
     */
    public String getSavedThreadId() {
        String threadId = prefs.getString("threadId", null);
        Log.d(TAG, "저장된 Thread ID 가져오기: " + threadId);
        return threadId;
    }

    public void createNewThreadId(String newThreadId) {
        long currentTime = Calendar.getInstance().getTimeInMillis();
        prefs.edit()
                .putString("threadId", newThreadId)
                .putLong("lastResetTime", currentTime) // 현재 시간 저장
                .apply();
    }

    // Thread ID 가져오기 또는 생성
    public void getOrCreateThreadId(ApiCallback<String> callback) {
        long currentTime = Calendar.getInstance().getTimeInMillis();
        long resetTime = getResetTime();
        long lastResetTime = prefs.getLong("lastResetTime", 0L);
        String threadId = prefs.getString("threadId", null);

        // 새벽 4:30 이후로 넘어갔는지 확인
        if (currentTime >= resetTime && (lastResetTime == 0L || lastResetTime < resetTime)) {
            clearThreadId(); // 초기화
            threadId = null;
        }

        if (threadId == null) {
            // Thread ID가 없으면 새로 생성
            chatApiManager.createThread(new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    Log.d(TAG, "Thread 생성 성공: " + result);

                    // 성공 시 SharedPreferences에 저장
                    saveThreadId(result);
                    callback.onSuccess(result);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Thread 생성 실패: " + errorMessage);
                    callback.onFailure(errorMessage);
                }
            });
        } else {
            // 기존 Thread ID 반환
            Log.d(TAG, "기존 Thread ID 사용: " + threadId);
            callback.onSuccess(threadId);
        }
    }

    /**
     * Thread ID 저장
     *
     * @param threadId 새로 생성된 Thread ID
     */
    private void saveThreadId(String threadId) {
        long currentTime = Calendar.getInstance().getTimeInMillis();
        prefs.edit()
                .putString("threadId", threadId)
                .putLong("lastResetTime", currentTime)
                .apply();
        Log.d(TAG, "Thread ID 저장 완료: " + threadId);
    }

    /**
     * Thread ID 초기화
     */
    public void clearThreadId() {
        prefs.edit()
                .remove("threadId")
                .remove("lastResetTime")
                .apply();
        Log.d(TAG, "Thread ID 초기화 완료");
    }

    /**
     * 오늘 새벽 4:30의 밀리초 값 반환
     *
     * @return 새벽 4:30의 밀리초 값
     */
    private long getResetTime() {
        Calendar resetTime = Calendar.getInstance();
        resetTime.set(Calendar.HOUR_OF_DAY, 4);
        resetTime.set(Calendar.MINUTE, 30);
        resetTime.set(Calendar.SECOND, 0);
        resetTime.set(Calendar.MILLISECOND, 0);
        return resetTime.getTimeInMillis();
    }

    /**
     * 초기 메시지 전송 여부 확인
     *
     * @param threadId 확인할 Thread ID
     * @return 메시지 전송 여부
     */
    public boolean isInitialMessageSent(String threadId) {
        return prefs.getBoolean("initialMessageSent_" + threadId, false);
    }

    /**
     * 초기 메시지 전송 여부 저장
     *
     * @param threadId 메시지 전송된 Thread ID
     */
    public void setInitialMessageSent(String threadId) {
        prefs.edit()
                .putBoolean("initialMessageSent_" + threadId, true)
                .apply();
    }
}
