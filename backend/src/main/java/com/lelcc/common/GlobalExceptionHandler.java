package com.lelcc.common;

import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    /** 路径没匹配到任何接口时返回 404，否则会被下面的 Exception 兜底吞成 500 */
    @ExceptionHandler(value = NoResourceFoundException.class)
    public Result<Void> notFoundException() {
        return Result.fail(404, "请求的接口不存在");
    }

    /** 上传文件超过 multipart 上限（spring.servlet.multipart.max-file-size）时的友好提示 */
    @ExceptionHandler(value = MaxUploadSizeExceededException.class)
    public Result<Void> uploadTooLargeException() {
        return Result.fail(400, "图片大小不能超过 5MB");
    }

    @ExceptionHandler(value = Exception.class)
    public Result<Void> commonException(Exception e) {
        e.printStackTrace();
        return Result.fail(500,"服务器错误:"+e.getMessage());

    }

}
