package com.example.melissa;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "https://api.openai.com/v1/";

    private static Retrofit retrofit;

    // Singleton 패턴으로 Retrofit 인스턴스 관리
    public static Retrofit getInstance() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create()) // JSON 직렬화/역직렬화
                    .build();
        }
        return retrofit;
    }
}
