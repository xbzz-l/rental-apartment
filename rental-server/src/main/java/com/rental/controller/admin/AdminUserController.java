package com.rental.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rental.common.Result;
import com.rental.entity.Booking;
import com.rental.entity.User;
import com.rental.service.BookingService;
import com.rental.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端-用户管理接口（需 admin 角色）
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final BookingService bookingService;

    /** 分页查询用户列表，支持账号/昵称/真实姓名模糊搜索 */
    @GetMapping
    public Result<IPage<User>> page(@RequestParam(defaultValue = "1") long page,
                                    @RequestParam(defaultValue = "10") long size,
                                    @RequestParam(required = false) String keyword) {
        return Result.ok(userService.page(page, size, keyword));
    }

    /** 用户数量统计 */
    @GetMapping("/count")
    public Result<Long> count() {
        return Result.ok(userService.count());
    }

    /** 查看指定用户的租房记录 */
    @GetMapping("/{id}/bookings")
    public Result<IPage<Booking>> userBookings(@PathVariable Long id,
                                               @RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size) {
        return Result.ok(bookingService.adminPage(page, size, id, null));
    }

    /** 删除用户（同时清理其租房记录，并释放不再被占用的房源） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }
}
