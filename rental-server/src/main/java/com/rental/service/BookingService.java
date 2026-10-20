package com.rental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rental.common.BusinessException;
import com.rental.dto.BookingCreateDTO;
import com.rental.entity.Apartment;
import com.rental.entity.Booking;
import com.rental.entity.User;
import com.rental.mapper.ApartmentMapper;
import com.rental.mapper.BookingMapper;
import com.rental.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 租房预约业务：提交预约（同步变更房源状态）、查询记录
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingMapper bookingMapper;
    private final ApartmentMapper apartmentMapper;
    private final UserMapper userMapper;
    private final ApartmentService apartmentService;

    /**
     * 提交租房预约（Demo 简化规则：预约即生效）
     * 1) 校验房源存在且状态为「可租」；2) 生成「已预约」记录；3) 房源状态变更为「已租」
     *
     * @param userId 当前登录用户ID
     * @param dto    预约参数（房源ID）
     * @return 生成的租房记录
     */
    @Transactional(rollbackFor = Exception.class)
    public Booking create(Long userId, BookingCreateDTO dto) {
        Apartment apartment = apartmentService.getById(dto.getApartmentId());
        if (!"可租".equals(apartment.getStatus())) {
            throw new BusinessException("该房源已被租，无法预约");
        }
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setApartmentId(dto.getApartmentId());
        booking.setStatus("已预约");
        booking.setCreateTime(LocalDateTime.now());
        bookingMapper.insert(booking);

        apartment.setStatus("已租");
        apartment.setUpdateTime(LocalDateTime.now());
        apartmentMapper.updateById(apartment);
        return booking;
    }

    /**
     * 取消租房预约（Demo 简化规则：仅「已预约」可取消，取消即释放房源）
     * 1) 校验记录存在且属于当前用户；2) 状态置为「已取消」；
     * 3) 若该房源下已无其他有效预约，则房源状态恢复为「可租」
     *
     * @param userId    当前登录用户ID
     * @param bookingId 租房记录ID
     * @return 取消后的租房记录
     */
    @Transactional(rollbackFor = Exception.class)
    public Booking cancel(Long userId, Long bookingId) {
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null || !booking.getUserId().equals(userId)) {
            throw new BusinessException("租房记录不存在");
        }
        if ("已取消".equals(booking.getStatus())) {
            throw new BusinessException("该预约已取消，无需重复操作");
        }
        if (!"已预约".equals(booking.getStatus())) {
            throw new BusinessException("当前状态不支持取消");
        }
        booking.setStatus("已取消");
        bookingMapper.updateById(booking);

        // 释放房源：仅当该房源下已无其他「已预约」记录时才恢复为「可租」
        apartmentService.releaseIfVacant(booking.getApartmentId());
        fillDisplay(Collections.singletonList(booking));
        return booking;
    }

    /**
     * 查询当前用户的租房记录（含房源冗余信息）
     *
     * @param userId 当前登录用户ID
     * @return 记录列表（按预约时间倒序）
     */
    public List<Booking> myList(Long userId) {
        List<Booking> list = bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getUserId, userId)
                .orderByDesc(Booking::getCreateTime));
        fillDisplay(list);
        return list;
    }

    /**
     * 分页查询租房记录（管理端），支持按用户/状态筛选
     *
     * @param page   页码（从1开始）
     * @param size   每页条数
     * @param userId 用户ID筛选，可为空
     * @param status 状态筛选，可为空
     * @return 分页结果（含房源与用户冗余信息）
     */
    public IPage<Booking> adminPage(long page, long size, Long userId, String status) {
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null, Booking::getUserId, userId)
                .eq(StringUtils.hasText(status), Booking::getStatus, status)
                .orderByDesc(Booking::getCreateTime);
        Page<Booking> result = bookingMapper.selectPage(new Page<>(page, size), wrapper);
        fillDisplay(result.getRecords());
        return result;
    }

    /**
     * 按预约时间范围分页查询记录（统计卡片下钻用）
     * 口径与统计卡片保持一致：不含「已取消」记录
     *
     * @param page  页码（从1开始）
     * @param size  每页条数
     * @param start 起始时间（含），可为空
     * @param end   结束时间（不含），可为空
     * @return 分页结果（含房源与用户冗余信息）
     */
    public IPage<Booking> pageByTimeRange(long page, long size, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(start != null, Booking::getCreateTime, start)
                .lt(end != null, Booking::getCreateTime, end)
                .ne(Booking::getStatus, "已取消")
                .orderByDesc(Booking::getCreateTime);
        Page<Booking> result = bookingMapper.selectPage(new Page<>(page, size), wrapper);
        fillDisplay(result.getRecords());
        return result;
    }

    /**
     * 填充记录的房源与用户冗余展示字段（标题/首图/户型/租金/用户账号）
     *
     * @param list 租房记录列表
     */
    private void fillDisplay(List<Booking> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> apartmentIds = list.stream().map(Booking::getApartmentId).collect(Collectors.toSet());
        Set<Long> userIds = list.stream().map(Booking::getUserId).collect(Collectors.toSet());

        Map<Long, Apartment> apartmentMap = apartmentMapper.selectBatchIds(apartmentIds).stream()
                .collect(Collectors.toMap(Apartment::getId, Function.identity()));
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        list.forEach(booking -> {
            Apartment apartment = apartmentMap.get(booking.getApartmentId());
            if (apartment != null) {
                booking.setApartmentTitle(apartment.getTitle());
                booking.setApartmentImage(firstImage(apartment.getImages()));
                booking.setLayout(apartment.getLayout());
                booking.setRent(apartment.getRent());
            }
            User user = userMap.get(booking.getUserId());
            if (user != null) {
                booking.setUsername(user.getUsername());
            }
        });
    }

    /** 取逗号分隔图片串的第一张 */
    private String firstImage(String images) {
        if (!StringUtils.hasText(images)) {
            return "";
        }
        return images.split(",")[0];
    }
}
