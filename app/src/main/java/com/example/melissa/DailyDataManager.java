package com.example.melissa;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class DailyDataManager {

    private static final String FILE_NAME = "daily_data.json";
    private final File storageFile;

    public DailyDataManager(Context context) {
        storageFile = new File(context.getFilesDir(), FILE_NAME);
        if (!storageFile.exists()) {
            try {
                storageFile.createNewFile();
                writeToFile(new JSONObject().toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public List<String> getAllKeys() {
        List<String> keys = new ArrayList<>();
        JSONObject allData = readFromFile();
        Iterator<String> iterator = allData.keys();
        while (iterator.hasNext()) {
            keys.add(iterator.next());
        }
        return keys;
    }

    public void addDailyData(DailyData dailyData) {
        try {
            JSONObject allData = readFromFile();
            String key = dailyData.getDateKey();

            // Convert daily data to JSON
            JSONObject dailyDataJson = dailyData.toJson();

            // Add to JSON file
            allData.put(key, dailyDataJson);
            writeToFile(allData.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public DailyData getDailyData(String dateKey) {
        try {
            JSONObject allData = readFromFile();
            if (allData.has(dateKey)) {
                return DailyData.fromJson(allData.getJSONObject(dateKey));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject readFromFile() {
        try {
            byte[] buffer = new byte[(int) storageFile.length()];
            FileInputStream fis = new FileInputStream(storageFile);
            fis.read(buffer);
            fis.close();
            return new JSONObject(new String(buffer));
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }

    private void writeToFile(String data) {
        try (FileWriter writer = new FileWriter(storageFile)) {
            writer.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class DailyData {
        private final int id;
        private final Date createdAt;
        private final Summary summary;

        public DailyData(int id, Date createdAt, Summary summary) {
            this.id = id;
            this.createdAt = createdAt;
            this.summary = summary;
        }

        public String getDateKey() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            return dateFormat.format(createdAt);
        }

        public JSONObject toJson() throws JSONException {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", id);
            jsonObject.put("created_at", createdAt.getTime());
            jsonObject.put("summary", summary.toJson());
            return jsonObject;
        }

        public static DailyData fromJson(JSONObject jsonObject) throws JSONException {
            int id = jsonObject.getInt("id");
            Date createdAt = new Date(jsonObject.getLong("created_at"));
            Summary summary = Summary.fromJson(jsonObject.getJSONObject("summary"));
            return new DailyData(id, createdAt, summary);
        }

        public static class Summary {
            private final JSONArray todayActivities;
            private final JSONArray gratitudePoints;
            private final JSONArray plans;
            private final float satisfactionScore;
            private final String satisfactionReason;

            public Summary(JSONArray todayActivities, JSONArray gratitudePoints, JSONArray plans,
                           float satisfactionScore, String satisfactionReason) {
                this.todayActivities = todayActivities;
                this.gratitudePoints = gratitudePoints;
                this.plans = plans;
                this.satisfactionScore = satisfactionScore;
                this.satisfactionReason = satisfactionReason;
            }

            public JSONObject toJson() throws JSONException {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("today_activities", todayActivities);
                jsonObject.put("gratitude_points", gratitudePoints);
                jsonObject.put("plans", plans);
                jsonObject.put("satisfaction_score", satisfactionScore);
                jsonObject.put("satisfaction_reason", satisfactionReason);
                return jsonObject;
            }

            public static Summary fromJson(JSONObject jsonObject) throws JSONException {
                JSONArray todayActivities = jsonObject.getJSONArray("today_activities");
                JSONArray gratitudePoints = jsonObject.getJSONArray("gratitude_points");
                JSONArray plans = jsonObject.getJSONArray("plans");
                float satisfactionScore = (float) jsonObject.getDouble("satisfaction_score");
                String satisfactionReason = jsonObject.getString("satisfaction_reason");
                return new Summary(todayActivities, gratitudePoints, plans, satisfactionScore, satisfactionReason);
            }
        }
    }
}

