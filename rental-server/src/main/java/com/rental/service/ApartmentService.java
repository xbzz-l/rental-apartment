package com.rental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rental.common.BusinessException;
import com.rental.dto.ApartmentDTO;
import com.rental.entity.Apartment;
import com.rental.entity.Booking;
import com.rental.mapper.ApartmentMapper;
import com.rental.mapper.BookingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 房源业务：列表查询（含筛选）、详情、增删改
 */
@Service
@RequiredArgsConstructor
public class ApartmentService {

    private final ApartmentMapper apartmentMapper;
    private final BookingMapper bookingMapper;

    /**
     * 分页查询房源列表，支持状态/户型/租金区间筛选
     *
     * @param page    页码（从1开始）
     * @param size    每页条数
     * @param status  状态筛选：可租/已租，可为空
     * @param layout  户型筛选，可为空
     * @param minRent 最低租金，可为空
     * @param maxRent 最高租金，可为空
     * @return 分页结果
     */
    public IPage<Apartment> page(long page, long size, String status, String layout, Integer minRent, Integer maxRent) {
        LambdaQueryWrapper<Apartment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(status), Apartment::getStatus, status)
                .eq(StringUtils.hasText(layout), Apartment::getLayout, layout)
                .ge(minRent != null, Apartment::getRent, minRent)
                .le(maxRent != null, Apartment::getRent, maxRent)
                .orderByDesc(Apartment::getUpdateTime);
        return apartmentMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 查询房源详情
     *
     * @param id 房源ID
     * @return 房源信息
     */
    public Apartment getById(Long id) {
        Apartment apartment = apartmentMapper.selectById(id);
        if (apartment == null) {
            throw new BusinessException("房源不存在");
        }
        return apartment;
    }

    /**
     * 新增房源（管理端），状态未指定时默认「可租」
     *
     * @param dto 房源参数
     * @return 新增后的房源
     */
    public Apartment create(ApartmentDTO dto) {
        Apartment apartment = new Apartment();
        copyFromDto(apartment, dto);
        if (!StringUtils.hasText(apartment.getStatus())) {
            apartment.setStatus("可租");
        }
        apartment.setCreateTime(LocalDateTime.now());
        apartment.setUpdateTime(LocalDateTime.now());
        apartmentMapper.insert(apartment);
        return apartment;
    }

    /**
     * 编辑房源（管理端）
     *
     * @param id  房源ID
     * @param dto 房源参数
     * @return 更新后的房源
     */
    public Apartment update(Long id, ApartmentDTO dto) {
        Apartment apartment = getById(id);
        copyFromDto(apartment, dto);
        apartment.setUpdateTime(LocalDateTime.now());
        apartmentMapper.updateById(apartment);
        return getById(id);
    }

    /**
     * 删除房源（管理端），同时删除该房源下的租房记录
     *
     * @param id 房源ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getById(id);
        bookingMapper.delete(new LambdaQueryWrapper<Booking>().eq(Booking::getApartmentId, id));
        apartmentMapper.deleteById(id);
    }

    /**
     * 若房源下已无「已预约」记录，则将其状态恢复为「可租」
     * 供「取消预约」「删除用户」等释放房源场景复用，避免误释放他人占用的房源
     *
     * @param apartmentId 房源ID
     */
    public void releaseIfVacant(Long apartmentId) {
        Long activeCount = bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getApartmentId, apartmentId)
                .eq(Booking::getStatus, "已预约"));
        if (activeCount != null && activeCount > 0) {
            return;
        }
        Apartment apartment = apartmentMapper.selectById(apartmentId);
        if (apartment != null && "已租".equals(apartment.getStatus())) {
            apartment.setStatus("可租");
            apartment.setUpdateTime(LocalDateTime.now());
            apartmentMapper.updateById(apartment);
        }
    }

    /** 将 DTO 字段复制到房源实体 */
    private void copyFromDto(Apartment apartment, ApartmentDTO dto) {
        apartment.setTitle(dto.getTitle());
        apartment.setImages(dto.getImages());
        apartment.setLayout(dto.getLayout());
        apartment.setArea(dto.getArea());
        apartment.setRent(dto.getRent());
        apartment.setStatus(dto.getStatus());
        apartment.setFacilities(dto.getFacilities());
        apartment.setAddress(dto.getAddress());
    }
}
