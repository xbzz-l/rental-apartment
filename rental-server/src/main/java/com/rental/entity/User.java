package com.rental.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体（对应 user 表）
 */
@Data
@TableName("`user`")
public class User {

    /** 用户ID（主键） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 账号（同时作为昵称） */
    private String username;

    /** 密码（BCrypt 加密存储，序列化时忽略） */
    @JsonIgnore
    private String password;

    /** 昵称（注册时默认等于账号，不支持单独修改） */
    private String nickname;

    /** 真实姓名 */
    private String realName;

    /** 年龄 */
    private Integer age;

    /** 联系电话 */
    private String phone;

    /** 同住人信息 */
    private String cohabitants;

    /** 头像URL */
    private String avatar;

    /** 角色：user 普通用户 / admin 管理员 */
    private String role;

    /** 注册时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
