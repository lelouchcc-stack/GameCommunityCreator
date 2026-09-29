package com.lelcc.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = BusinessException.class)
    public Result<Void> businessException(BusinessException e){
        return Result.fail(e.getErrorCode(), e.getMessage());
    }
    @ExceptionHandler(value = Exception.class)
    public Result<Void> commonException(Exception e) {
        e.printStackTrace();
        return Result.fail(500,"服务器错误:"+e.getMessage());

    }

}
