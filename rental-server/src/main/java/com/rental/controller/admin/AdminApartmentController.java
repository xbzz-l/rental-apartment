package com.rental.controller.admin;

import com.rental.common.Result;
import com.rental.dto.ApartmentDTO;
import com.rental.entity.Apartment;
import com.rental.service.ApartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端-房源管理接口（需 admin 角色）：新增 / 编辑 / 删除
 */
@RestController
@RequestMapping("/api/admin/apartments")
@RequiredArgsConstructor
public class AdminApartmentController {

    private final ApartmentService apartmentService;

    /** 新增房源 */
    @PostMapping
    public Result<Apartment> create(@Valid @RequestBody ApartmentDTO dto) {
        return Result.ok(apartmentService.create(dto));
    }

    /** 编辑房源 */
    @PutMapping("/{id}")
    public Result<Apartment> update(@PathVariable Long id, @Valid @RequestBody ApartmentDTO dto) {
        return Result.ok(apartmentService.update(id, dto));
    }

    /** 删除房源（同步删除关联租房记录） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        apartmentService.delete(id);
        return Result.ok();
    }
}
