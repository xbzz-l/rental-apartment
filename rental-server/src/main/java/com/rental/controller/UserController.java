package com.rental.controller;

import com.rental.common.Result;
import com.rental.dto.UserUpdateDTO;
import com.rental.entity.User;
import com.rental.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口：个人信息查询与修改（需登录）
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 查询当前登录用户信息 */
    @GetMapping("/info")
    public Result<User> info(@RequestAttribute("userId") Long userId) {
        return Result.ok(userService.getById(userId));
    }

    /** 修改个人信息（真实姓名/年龄/电话/同住人信息/头像） */
    @PutMapping("/info")
    public Result<User> update(@RequestAttribute("userId") Long userId, @Valid @RequestBody UserUpdateDTO dto) {
        return Result.ok(userService.updateInfo(userId, dto));
    }
}
