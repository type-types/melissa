package com.example.melissa.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class DatabaseHelper {

    private static final String DATABASE_NAME = "chat_summaries.db";
    private SQLiteDatabase database;
    private Context context;

    public DatabaseHelper(Context context) {
        this.context = context;
    }

    public void openDatabase() {
        // Build the full path to the database
        String databasePath = context.getDatabasePath(DATABASE_NAME).getPath();
        database = SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.OPEN_READONLY);
    }

    public Map<String, String> getTitlesForMonth(int year, int month) {
        Map<String, String> titlesMap = new HashMap<>();

        // Format the month to ensure it's two digits
        String monthStr = String.format("%02d", month + 1);

        String datePattern = year + "-" + monthStr + "-%";

        String query = "SELECT date, summary_json FROM summaries WHERE date LIKE ?";
        Cursor cursor = database.rawQuery(query, new String[]{datePattern});

        if (cursor != null && cursor.moveToFirst()) {
            // Cache column indices
            int dateColIndex = cursor.getColumnIndex("date");
            int summaryJsonColIndex = cursor.getColumnIndex("summary_json");

            // Check if columns exist
            if (dateColIndex == -1 || summaryJsonColIndex == -1) {
                // Log an error message or handle accordingly
                Log.e("DatabaseHelper", "Required columns are missing from the result set.");
                cursor.close();
                return titlesMap; // or throw an exception if preferred
            }

            do {
                String date = cursor.getString(dateColIndex);
                String summaryJson = cursor.getString(summaryJsonColIndex);

                String title = "";
                try {
                    JSONObject jsonObject = new JSONObject(summaryJson);
                    title = jsonObject.getString("title");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                titlesMap.put(date, title);
            } while (cursor.moveToNext());
            cursor.close();
        } else {
            if (cursor != null) {
                cursor.close();
            }
        }

        return titlesMap;
    }

    public void closeDatabase() {
        if (database != null) {
            database.close();
        }
    }
}