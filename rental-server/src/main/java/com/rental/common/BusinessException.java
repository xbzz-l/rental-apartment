package com.rental.common;

import lombok.Getter;

/**
 * 业务异常：业务校验不通过时抛出，由全局异常处理器统一转换为响应
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 异常码，默认 400 */
    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
