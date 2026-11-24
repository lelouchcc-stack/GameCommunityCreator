package com.gcc.community.common;

import java.util.Map;

// 统一返回格式：code、message、data、fieldErrors。
public record ApiResponse<T>(int code, String message, T data, Map<String, String> fieldErrors) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "OK", data, null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(201, "Created", data, null);
    }

    public static ApiResponse<Void> noContent() {
        return new ApiResponse<>(204, "No Content", null, null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, null);
    }

    public static <T> ApiResponse<T> validationError(String message, Map<String, String> fieldErrors) {
        return new ApiResponse<>(400, message, null, fieldErrors);
    }
}
