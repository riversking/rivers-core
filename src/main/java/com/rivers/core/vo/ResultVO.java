package com.rivers.core.vo;

import lombok.Data;

@Data
public class ResultVO<T> {

    private Integer code;

    private String message;

    private T data;

    public ResultVO(T data) {
        this.data = data;
    }

    public ResultVO(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public ResultVO(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ResultVO<T> ok(Integer code, String message) {
        return new ResultVO<>(code, message);
    }

    public static <T> ResultVO<T> ok() {
        return new ResultVO<>(200, "操作成功");
    }

    public static <T> ResultVO<T> ok(T data) {
        return new ResultVO<>(200, "操作成功", data);
    }

    public static <T> ResultVO<T> ok(Integer code, String message, T data) {
        return new ResultVO<>(code, message, data);
    }
}
