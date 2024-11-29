package com.example.melissa.database;

public class Summary {
    private String date; // YYYY-MM-DD 형식의 날짜
    private String summaryJson; // 요약 데이터를 담은 JSON 문자열
    private String conversationJson; // 대화 데이터를 담은 JSON 문자열

    public Summary(String date, String summaryJson, String conversationJson) {
        this.date = date;
        this.summaryJson = summaryJson;
        this.conversationJson = conversationJson;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getSummaryJson() {
        return summaryJson;
    }

    public void setSummaryJson(String summaryJson) {
        this.summaryJson = summaryJson;
    }

    public String getConversationJson() {
        return conversationJson;
    }

    public void setConversationJson(String conversationJson) {
        this.conversationJson = conversationJson;
    }

    @Override
    public String toString() {
        return "Summary{" +
                "date='" + date + '\'' +
                ", summaryJson='" + summaryJson + '\'' +
                ", conversationJson='" + conversationJson + '\'' +
                '}';
    }
}
