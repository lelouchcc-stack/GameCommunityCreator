package com.lelcc.common;

import lombok.Data;

@Data
public class BusinessException extends RuntimeException {
    private int errorCode;


    public BusinessException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
