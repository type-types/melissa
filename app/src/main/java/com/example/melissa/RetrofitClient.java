package com.example.melissa;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;

public class RetrofitClient {
    private static final String BASE_URL = "https://api.openai.com/v1/";

    private static Retrofit retrofit;

    public static Retrofit getInstance() {
        if (retrofit == null) {
            // 로깅 인터셉터 추가
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY); // 요청/응답 상세 로그

            // 공통 헤더 추가 인터셉터
            Interceptor headerInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request original = chain.request();
                    Request request = original.newBuilder()
                            .header("Authorization", "Bearer " + BuildConfig.OPENAI_API_KEY) // 공통 Authorization 헤더
                            .header("OpenAI-Beta", "assistants=v2") // 공통 OpenAI-Beta 헤더
                            .build();
                    return chain.proceed(request);
                }
            };

            // OkHttpClient 빌더
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging) // 로깅 추가
                    .addInterceptor(headerInterceptor) // 공통 헤더 추가
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS) // 연결 타임아웃
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS) // 읽기 타임아웃
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS) // 쓰기 타임아웃
                    .build();

            // Retrofit 빌더
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create()) // JSON 직렬화/역직렬화
                    .client(client) // 커스텀 OkHttpClient 추가
                    .build();
        }
        return retrofit;
    }
}
