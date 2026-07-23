package com.knowai.knowaibackend.common;

import lombok.Data;

@Data
public class Result<T>{

    private Integer code;

    private String message;

    private T data;

    /**
     * 成功，无返回数据
     */
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        return result;
    }

    /**
     * 成功，携带返回数据
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    /**
     * 成功，自定义提示文字+数据
     */
    public static <T> Result<T> success(String msg, T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(msg);
        result.setData(data);
        return result;
    }

    /**
     * 失败，默认500
     */
    public static <T> Result<T> fail(String msg) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(msg);
        return result;
    }

    /**
     * 自定义状态码+失败信息（401/403等）
     */
    public static <T> Result<T> fail(Integer code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(msg);
        return result;
    }

}
