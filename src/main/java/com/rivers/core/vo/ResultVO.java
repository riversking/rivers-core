package com.rivers.core.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"code", "message", "data"})
public class ResultVO<T extends Serializable> implements Serializable {


    @Serial
    private static final long serialVersionUID = 3278715392832902343L;

    private Integer code;

    private String message;

    private T data;

    public ResultVO() {
    }

    public ResultVO(T data) {
        this.data = data;
    }

    public ResultVO(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public ResultVO(Integer code, String message, T data) {
        this(code, message);
        this.data = data;
    }

    public static ResultVO<EmptyType> ok() {
        return new ResultVO<>(200, "操作成功");
    }


    public static <T extends Serializable> ResultVO<T> ok(Integer code, String message) {
        return new ResultVO<>(code, message);
    }



    public static <T extends Serializable> ResultVO<T> ok(T data) {
        return new ResultVO<>(200, "操作成功", data);
    }

    public static <T extends Serializable> ResultVO<T> ok(Integer code, String message, T data) {
        return new ResultVO<>(code, message, data);
    }

    public static <T extends Serializable> ResultVO<T> fail(Integer code, String message) {
        return new ResultVO<>(code, message);
    }

    public abstract static class EmptyType implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }
}
