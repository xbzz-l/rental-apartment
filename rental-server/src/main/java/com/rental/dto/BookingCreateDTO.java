package com.rental.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 租房预约请求参数
 */
@Data
public class BookingCreateDTO {

    /** 预约房源ID */
    @NotNull(message = "房源ID不能为空")
    private Long apartmentId;
}
