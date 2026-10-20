package com.rental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rental.entity.Apartment;
import com.rental.entity.Booking;
import com.rental.mapper.ApartmentMapper;
import com.rental.mapper.BookingMapper;
import com.rental.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据统计业务：管理端业务统计 + BI 看板图表数据
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final UserMapper userMapper;
    private final ApartmentMapper apartmentMapper;
    private final BookingMapper bookingMapper;

    /**
     * 管理端业务统计总览
     *
     * @return Map：userCount 用户总数、apartmentCount 房源总数、rentedCount 已租、vacantCount 空置、
     *         todayBookings 今日预约量、weekBookings 本周预约量（预约量均不含「已取消」记录）
     */
    public Map<String, Object> overview() {
        Map<String, Object> data = new HashMap<>();
        data.put("userCount", userMapper.selectCount(null));
        data.put("apartmentCount", apartmentMapper.selectCount(null));
        data.put("rentedCount", apartmentMapper.selectCount(
                new LambdaQueryWrapper<Apartment>().eq(Apartment::getStatus, "已租")));
        data.put("vacantCount", apartmentMapper.selectCount(
                new LambdaQueryWrapper<Apartment>().eq(Apartment::getStatus, "可租")));

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
        data.put("todayBookings", bookingMapper.selectCount(
                new LambdaQueryWrapper<Booking>()
                        .ge(Booking::getCreateTime, todayStart)
                        .ne(Booking::getStatus, "已取消")));
        data.put("weekBookings", bookingMapper.selectCount(
                new LambdaQueryWrapper<Booking>()
                        .ge(Booking::getCreateTime, weekStart)
                        .ne(Booking::getStatus, "已取消")));
        return data;
    }

    /**
     * BI 看板统计数据（以「房源 + 营业额」为核心视角）
     *
     * @return Map：totalApartments 总房源数、vacantCount 待租房源数、
     *         monthRevenue 当月营业额、dayRevenue 当日营业额、lastMonthRevenue 上月营业额、
     *         monthGrowth 环比增长率（%，上月为 0 时返回 null）、
     *         revenueTrend 近6个月营业额 [{month,amount}]、dailyRevenue 近7天营业额 [{day,amount}]、
     *         statusPie 房源占比 [{name,value}]
     */
    public Map<String, Object> bi() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalApartments", apartmentMapper.selectCount(null));
        data.put("vacantCount", apartmentMapper.selectCount(
                new LambdaQueryWrapper<Apartment>().eq(Apartment::getStatus, "可租")));

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        long monthRevenue = revenueBetween(monthStart, monthStart.plusMonths(1));
        long lastMonthRevenue = revenueBetween(monthStart.minusMonths(1), monthStart);
        long dayRevenue = revenueBetween(today, today.plusDays(1));

        data.put("monthRevenue", monthRevenue);
        data.put("dayRevenue", dayRevenue);
        data.put("lastMonthRevenue", lastMonthRevenue);
        data.put("monthGrowth", growthRate(monthRevenue, lastMonthRevenue));
        data.put("revenueTrend", revenueTrend());
        data.put("dailyRevenue", dailyRevenue());
        data.put("statusPie", statusPie());
        return data;
    }

    /** 统计 [start, end) 区间的营业额（按预约房源月租金合计，不含已取消） */
    private long revenueBetween(LocalDate start, LocalDate end) {
        return bookingMapper.selectRevenueBetween(start.atStartOfDay(), end.atStartOfDay());
    }

    /**
     * 环比增长率（保留 1 位小数，单位 %）
     * 上月为 0 时无法计算，返回 null 由前端按「上月无营业额」展示
     */
    private Double growthRate(long current, long last) {
        if (last <= 0) {
            return null;
        }
        return Math.round((current - last) * 1000.0 / last) / 10.0;
    }

    /** 近 6 个月每月营业额，缺失月份补 0 */
    private List<Map<String, Object>> revenueTrend() {
        Map<String, Object> raw = bookingMapper.selectMonthlyRevenue().stream()
                .collect(Collectors.toMap(m -> String.valueOf(m.get("month")), m -> m.get("amount")));
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            String month = now.minusMonths(i).format(DateTimeFormatter.ofPattern("yyyy-MM"));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", month);
            item.put("amount", raw.getOrDefault(month, 0L));
            result.add(item);
        }
        return result;
    }

    /** 近 7 天每日营业额，缺失日期补 0 */
    private List<Map<String, Object>> dailyRevenue() {
        Map<String, Object> raw = bookingMapper.selectDailyRevenue().stream()
                .collect(Collectors.toMap(m -> String.valueOf(m.get("day")), m -> m.get("amount")));
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            String day = now.minusDays(i).format(DateTimeFormatter.ofPattern("MM-dd"));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("day", day);
            item.put("amount", raw.getOrDefault(day, 0L));
            result.add(item);
        }
        return result;
    }

    /** 房源状态占比：已租 / 空置 */
    private List<Map<String, Object>> statusPie() {
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(pieItem("已租", apartmentMapper.selectCount(
                new LambdaQueryWrapper<Apartment>().eq(Apartment::getStatus, "已租"))));
        result.add(pieItem("空置", apartmentMapper.selectCount(
                new LambdaQueryWrapper<Apartment>().eq(Apartment::getStatus, "可租"))));
        return result;
    }

    /** 构造饼图单项 */
    private Map<String, Object> pieItem(String name, long value) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("value", value);
        return item;
    }
}
