package com.rental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rental.entity.Booking;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 租房记录表 Mapper
 */
public interface BookingMapper extends BaseMapper<Booking> {

    /**
     * 查询近 6 个月每月营业额（按预约房源的月租金合计，不含已取消）
     * Map key：month=2026-05，amount=金额
     */
    @Select("SELECT DATE_FORMAT(b.create_time, '%Y-%m') AS month, " +
            "CAST(COALESCE(SUM(a.rent), 0) AS SIGNED) AS amount " +
            "FROM `booking` b JOIN `apartment` a ON a.id = b.apartment_id " +
            "WHERE b.create_time >= DATE_SUB(CURDATE(), INTERVAL 5 MONTH) AND b.`status` <> '已取消' " +
            "GROUP BY month ORDER BY month")
    List<Map<String, Object>> selectMonthlyRevenue();

    /**
     * 查询近 7 天每日营业额（按预约房源的月租金合计，不含已取消）
     * Map key：day=10-05，amount=金额
     */
    @Select("SELECT DATE_FORMAT(b.create_time, '%m-%d') AS `day`, " +
            "CAST(COALESCE(SUM(a.rent), 0) AS SIGNED) AS amount " +
            "FROM `booking` b JOIN `apartment` a ON a.id = b.apartment_id " +
            "WHERE b.create_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) AND b.`status` <> '已取消' " +
            "GROUP BY `day` ORDER BY `day`")
    List<Map<String, Object>> selectDailyRevenue();

    /**
     * 统计指定时间区间内的营业额（按预约房源的月租金合计，不含已取消）
     *
     * @param start 起始时间（含）
     * @param end   结束时间（不含）
     * @return 营业额（元）
     */
    @Select("SELECT CAST(COALESCE(SUM(a.rent), 0) AS SIGNED) " +
            "FROM `booking` b JOIN `apartment` a ON a.id = b.apartment_id " +
            "WHERE b.create_time >= #{start} AND b.create_time < #{end} AND b.`status` <> '已取消'")
    long selectRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
