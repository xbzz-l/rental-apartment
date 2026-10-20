package com.rental.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rental.common.Result;
import com.rental.entity.Booking;
import com.rental.service.BookingService;
import com.rental.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 管理端-统计接口（需 admin 角色）：业务统计总览 + 统计下钻明细 + BI 看板数据
 */
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final StatsService statsService;
    private final BookingService bookingService;

    /** 业务统计总览：用户/房源/预约量 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(statsService.overview());
    }

    /** BI 看板统计数据：KPI + 趋势图 + 占比图 */
    @GetMapping("/bi")
    public Result<Map<String, Object>> bi() {
        return Result.ok(statsService.bi());
    }

    /**
     * 预约明细下钻（业务统计卡片点击查看）
     *
     * @param range today=今日、week=本周，默认今日
     * @param page  页码（从1开始）
     * @param size  每页条数
     * @return 该时间范围内的预约记录（不含已取消，与卡片数字口径一致）
     */
    @GetMapping("/bookings")
    public Result<IPage<Booking>> bookingDetail(@RequestParam(defaultValue = "today") String range,
                                                @RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = "week".equalsIgnoreCase(range)
                ? today.with(DayOfWeek.MONDAY).atStartOfDay()
                : today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        return Result.ok(bookingService.pageByTimeRange(page, size, start, end));
    }
}
