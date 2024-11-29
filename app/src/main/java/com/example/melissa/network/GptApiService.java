package com.example.melissa.network;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface GptApiService {

    // Create Thread API
    @POST("threads")
    Call<JsonObject> createThread(
            @Header("Authorization") String authorization,
            @Header("OpenAI-Beta") String betaVersion,
            @Body JsonObject body
    );

    // Create Message API
    @POST("threads/{thread_id}/messages")
    Call<JsonObject> createMessage(
            @Header("Authorization") String authorization,
            @Header("OpenAI-Beta") String betaVersion,
            @Path("thread_id") String threadId,
            @Body JsonObject body
    );

    // Create Run API
    @POST("threads/{thread_id}/runs")
    Call<JsonObject> createRun(
            @Header("Authorization") String authorization,
            @Header("OpenAI-Beta") String betaVersion,
            @Path("thread_id") String threadId,
            @Body JsonObject body
    );

    // Retrieve Run API (thread_id 추가)
    @GET("threads/{thread_id}/runs/{run_id}")
    Call<JsonObject> retrieveRun(
            @Header("Authorization") String authorization,
            @Header("OpenAI-Beta") String betaVersion,
            @Path("thread_id") String threadId,
            @Path("run_id") String runId
    );

    // List Messages API
    @GET("threads/{thread_id}/messages")
    Call<JsonObject> listMessages(
            @Header("Authorization") String authorization,
            @Header("OpenAI-Beta") String betaVersion,
            @Path("thread_id") String threadId
    );

    // General GPT Request API
    @POST("chat/completions") // Base URL에서 "v1/" 이미 포함
    Call<JsonObject> generalGptRequest(
            @Header("Authorization") String authorization,
            @Body JsonObject body
    );
}
