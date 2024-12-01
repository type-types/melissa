package com.example.melissa.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SQLiteHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "chat_summaries.db"; // 데이터베이스 이름
    private static final int DATABASE_VERSION = 1; // 데이터베이스 버전

    // 테이블 이름 및 컬럼 이름
    private static final String TABLE_NAME = "summaries";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_SUMMARY_JSON = "summary_json";
    private static final String COLUMN_FULL_CONVERSATION_JSON = "full_conversation_json";

    // 로그 태그
    private static final String TAG = "SQLiteHelper";

    // 생성자
    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 테이블 생성 SQL
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_DATE + " TEXT PRIMARY KEY, " + // 날짜를 기본 키로 설정
                COLUMN_SUMMARY_JSON + " TEXT NOT NULL, " +
                COLUMN_FULL_CONVERSATION_JSON + " TEXT NOT NULL)";
        try {
            db.execSQL(createTable); // SQL 실행
            Log.d(TAG, "테이블 생성 완료: " + TABLE_NAME);
        } catch (Exception e) {
            Log.e(TAG, "테이블 생성 중 오류 발생", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 기존 테이블 삭제 후 재생성
        try {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
            Log.d(TAG, "테이블 업그레이드 완료: " + TABLE_NAME);
        } catch (Exception e) {
            Log.e(TAG, "테이블 업그레이드 중 오류 발생", e);
        }
    }

    /**
     * 데이터를 삽입하거나 업데이트하는 메서드.
     *
     * @param summary Summary 객체
     */
    public void upsertSummary(Summary summary) {
        SQLiteDatabase db = null;
        try {
            String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(COLUMN_DATE, todayDate);
            values.put(COLUMN_SUMMARY_JSON, summary.getSummaryJson());
            values.put(COLUMN_FULL_CONVERSATION_JSON, summary.getConversationJson());

            // 기존 데이터를 덮어쓰기 위해 REPLACE 사용
            long rowId = db.replace(TABLE_NAME, null, values);
            if (rowId != -1) {
                Log.d(TAG, "데이터 저장 완료. Row ID: " + rowId);
            } else {
                Log.e(TAG, "데이터 저장 실패");
            }
        } catch (Exception e) {
            Log.e(TAG, "데이터 저장 중 오류 발생", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    /**
     * 특정 날짜의 데이터를 Summary 객체로 조회하는 메서드.
     *
     * @param date 조회할 날짜 (YYYY-MM-DD 형식)
     * @return Summary 객체 또는 null (데이터가 없는 경우)
     */
    public Summary getSummaryByDate(String date) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        Summary summary = null;

        try {
            db = this.getReadableDatabase();

            String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_DATE + "=?";
            cursor = db.rawQuery(query, new String[]{date});

            if (cursor != null && cursor.moveToFirst()) {
                String summaryJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUMMARY_JSON));
                String conversationJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FULL_CONVERSATION_JSON));
                summary = new Summary(date, summaryJson, conversationJson);
                Log.d(TAG, "Summary 조회 성공: " + summary);
            } else {
                Log.d(TAG, "Summary 데이터가 존재하지 않음: 날짜 = " + date);
            }
        } catch (Exception e) {
            Log.e(TAG, "Summary 조회 중 오류 발생", e);
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        return summary;
    }

    public int getRowCount() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        int rowCount = 0;

        try {
            db = this.getReadableDatabase();

            // 총 행 개수를 계산하는 쿼리 실행
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null);

            if (cursor != null && cursor.moveToFirst()) {
                rowCount = cursor.getInt(0); // 첫 번째 열의 값을 가져옴
            }
        } catch (Exception e) {
            Log.e(TAG, "행 개수 계산 중 오류 발생", e);
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        return rowCount;
    }

}
