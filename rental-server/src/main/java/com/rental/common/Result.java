package com.rental.common;

import lombok.Data;

/**
 * 统一响应结果封装
 *
 * @param <T> 业务数据类型
 */
@Data
public class Result<T> {

    /** 状态码：200 成功，400 业务错误，401 未登录，403 无权限，500 系统异常 */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    /** 构造成功响应 */
    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    /** 构造无数据的成功响应 */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    /** 构造失败响应 */
    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
