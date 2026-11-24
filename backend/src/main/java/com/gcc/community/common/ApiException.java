package com.gcc.community.common;

import java.util.Map;

// 业务异常，带状态码和提示信息。
public class ApiException extends RuntimeException {

    private final int status;
    private final Map<String, String> fieldErrors;

    public ApiException(int status, String message) {
        this(status, message, null);
    }

    public ApiException(int status, String message, Map<String, String> fieldErrors) {
        super(message);
        this.status = status;
        this.fieldErrors = fieldErrors;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(400, message);
    }

    public static ApiException badRequest(String message, Map<String, String> fieldErrors) {
        return new ApiException(400, message, fieldErrors);
    }

    public static ApiException notFound(String message) {
        return new ApiException(404, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(409, message);
    }

    public int getStatus() {
        return status;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
