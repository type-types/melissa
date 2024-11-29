package com.example.melissa.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SQLiteHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "chat_summaries.db"; // 데이터베이스 이름
    private static final int DATABASE_VERSION = 1; // 데이터베이스 버전

    // 테이블 이름 및 컬럼 이름
    private static final String TABLE_NAME = "summaries";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TIMESTAMP = "timestamp";
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
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
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
     * 요약 데이터를 데이터베이스에 삽입하는 메서드.
     *
     * @param summaryJson 요약 내용 (JSON 직렬화된 문자열)
     * @param fullConversationJson 전체 대화 내용 (JSON 직렬화된 문자열)
     */
    public void insertSummary(String summaryJson, String fullConversationJson) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase(); // 쓰기 가능한 DB 가져오기

            ContentValues values = new ContentValues();
            values.put(COLUMN_SUMMARY_JSON, summaryJson); // 요약 JSON
            values.put(COLUMN_FULL_CONVERSATION_JSON, fullConversationJson); // 전체 대화 JSON

            long rowId = db.insert(TABLE_NAME, null, values); // 데이터 삽입
            if (rowId != -1) {
                Log.d(TAG, "새로운 요약 저장 완료. Row ID: " + rowId);
            } else {
                Log.e(TAG, "데이터 삽입 실패");
            }
        } catch (Exception e) {
            Log.e(TAG, "데이터 삽입 중 오류 발생", e);
        } finally {
            if (db != null) {
                db.close(); // DB 연결 닫기
            }
        }
    }

    /**
     * 저장된 요약 데이터를 조회하는 메서드.
     *
     * @return Cursor 객체 (데이터 조회 결과)
     */
    public Cursor getSummaries() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = this.getReadableDatabase(); // 읽기 가능한 DB 가져오기

            String query = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";
            cursor = db.rawQuery(query, null); // 결과 반환
            Log.d(TAG, "요약 데이터 조회 성공. 총 데이터 수: " + cursor.getCount());
        } catch (Exception e) {
            Log.e(TAG, "데이터 조회 중 오류 발생", e);
        }
        return cursor;
    }

    /**
     * 특정 ID로 저장된 데이터를 삭제하는 메서드.
     *
     * @param id 삭제할 데이터의 ID
     */
    public void deleteSummary(int id) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase(); // 쓰기 가능한 DB 가져오기

            int rowsAffected = db.delete(TABLE_NAME, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
            if (rowsAffected > 0) {
                Log.d(TAG, "삭제 완료. 삭제된 행 수: " + rowsAffected);
            } else {
                Log.w(TAG, "삭제할 데이터가 존재하지 않음. ID: " + id);
            }
        } catch (Exception e) {
            Log.e(TAG, "데이터 삭제 중 오류 발생", e);
        } finally {
            if (db != null) {
                db.close(); // DB 연결 닫기
            }
        }
    }
}
