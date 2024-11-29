package com.example.melissa.network;

/**
 * API 응답을 처리하기 위한 제네릭 콜백 인터페이스.
 * @param <T> 성공 시 반환되는 결과의 타입.
 */
public interface ApiCallback<T> {
    /**
     * API 호출이 성공했을 때 호출됩니다.
     * @param result 성공한 API 응답의 결과.
     */
    void onSuccess(T result);

    /**
     * API 호출이 실패했을 때 호출됩니다.
     * @param errorMessage 실패에 대한 에러 메시지.
     */
    void onFailure(String errorMessage);
}
