package com.lelcc.common;

import lombok.Getter;
import lombok.Setter;

/**
 * 统一响应体。字段必须有 getter，否则 Jackson 找不到可读属性、序列化时直接报错。
 */
@Getter
@Setter
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
