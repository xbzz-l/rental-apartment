package com.rental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求参数
 * 注册只需账号与密码，昵称统一取账号，真实信息在「编辑资料」中完善
 */
@Data
public class RegisterDTO {

    /** 账号 */
    @NotBlank(message = "账号不能为空")
    private String username;

    /** 密码（6-20位） */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需为6-20位")
    private String password;
}
