package com.lelcc.common;

public class Result<T> {
    private T data;
    private String message;
    private int code;

    public Result() {

    }
    public static <T>Result<T> success(T data) {
        Result<T> result = new Result<T>();
        result.data = data;
        result.message = "success";
        result.code = 200;
        return result;
    }
    public static <T>Result<T> fail(int errorCode, String message) {
        Result<T> result = new Result<T>();
        result.code = errorCode;
        result.message = message;
        return result;
    }
}
