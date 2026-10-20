package com.rental.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 个人信息更新参数（真实姓名 / 年龄 / 电话 / 同住人信息 / 头像，均可选）
 * 昵称不在此列——昵称统一等于账号，不允许单独修改
 */
@Data
public class UserUpdateDTO {

    /** 真实姓名 */
    private String realName;

    /** 年龄 */
    @Min(value = 1, message = "年龄需大于 0")
    @Max(value = 120, message = "年龄需小于 120")
    private Integer age;

    /** 联系电话 */
    private String phone;

    /** 同住人信息 */
    private String cohabitants;

    /** 头像URL */
    private String avatar;
}
