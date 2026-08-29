package com.knowai.knowaibackend.exception;

public class BusinessException extends RuntimeException{
    // 构造方法：接收异常提示信息
    public BusinessException(String message) {
        super(message);
    }
}
