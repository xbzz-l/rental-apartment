package com.rental.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rental.common.Result;
import com.rental.entity.Apartment;
import com.rental.service.ApartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 房源接口：列表与详情（公开访问）
 */
@RestController
@RequestMapping("/api/apartments")
@RequiredArgsConstructor
public class ApartmentController {

    private final ApartmentService apartmentService;

    /**
     * 分页查询房源列表，支持状态/户型/租金区间筛选
     */
    @GetMapping
    public Result<IPage<Apartment>> page(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String layout,
                                         @RequestParam(required = false) Integer minRent,
                                         @RequestParam(required = false) Integer maxRent) {
        return Result.ok(apartmentService.page(page, size, status, layout, minRent, maxRent));
    }

    /** 查询房源详情 */
    @GetMapping("/{id}")
    public Result<Apartment> detail(@PathVariable Long id) {
        return Result.ok(apartmentService.getById(id));
    }
}
