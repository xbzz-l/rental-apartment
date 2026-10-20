package com.rental.controller;

import com.rental.common.Result;
import com.rental.dto.BookingCreateDTO;
import com.rental.entity.Booking;
import com.rental.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 租房预约接口（需登录）
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /** 提交租房预约（预约即生效，房源变更为已租） */
    @PostMapping
    public Result<Booking> create(@RequestAttribute("userId") Long userId, @Valid @RequestBody BookingCreateDTO dto) {
        return Result.ok(bookingService.create(userId, dto));
    }

    /** 查询当前用户的租房记录 */
    @GetMapping("/my")
    public Result<List<Booking>> myList(@RequestAttribute("userId") Long userId) {
        return Result.ok(bookingService.myList(userId));
    }

    /** 取消租房预约（仅「已预约」可取消，取消后释放房源） */
    @PutMapping("/{id}/cancel")
    public Result<Booking> cancel(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        return Result.ok(bookingService.cancel(userId, id));
    }
}
