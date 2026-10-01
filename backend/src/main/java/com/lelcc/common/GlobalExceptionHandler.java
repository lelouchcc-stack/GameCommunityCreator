package com.lelcc.common;

import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = BusinessException.class)
    public Result<Void> businessException(BusinessException e){
        return Result.fail(e.getErrorCode(), e.getMessage());
    }

    /** @Valid 校验失败时返回具体字段的提示，而不是笼统的 500 */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public Result<Void> validationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("请求参数不合法");
        return Result.fail(400, message);
    }

    @ExceptionHandler(value = Exception.class)
    public Result<Void> commonException(Exception e) {
        e.printStackTrace();
        return Result.fail(500,"服务器错误:"+e.getMessage());

    }

}
