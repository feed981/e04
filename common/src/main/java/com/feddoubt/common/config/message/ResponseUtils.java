package com.feddoubt.common.config.message;

import org.springframework.http.ResponseEntity;

public class ResponseUtils {

	public static <T> ApiResponse<T> success(int code, String message, T data) {
		return ApiResponse.<T>builder().code(code).message(message).success(true).data(data).build();
	}
	
    public static <T> ApiResponse<T> success(int code, T data) {
        return success(code, "系統成功", data);
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(200, data);
    }

    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder().code(200).success(true).build();
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder().code(code).message(message).success(false).build();
    }

	public static <T> ApiResponse<T> error(int code, String message, T data) {
		return ApiResponse.<T>builder().code(code).message(message).success(false).data(data).build();
	}
	
    public static <T> ApiResponse<T> error(int code, T data) {
        return error(code, "系統錯誤", data);
    }

    public static <T> ApiResponse<T> error(T data) {
        return error(500, data); 
    }

    public static <T> ApiResponse<T> error() {
        return ApiResponse.<T>builder().code(500).success(false).build();
    }

    public static <T> ResponseEntity<ApiResponse<T>> httpStatus2ApiResponse(ApiStatus httpStatus){
        return ResponseEntity.ok(
                error(
                        httpStatus.value(),
                        httpStatus.getReasonPhrase()
                )
        );
    }

}