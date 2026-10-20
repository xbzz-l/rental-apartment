package com.rental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 房源新增/编辑参数（管理端）
 */
@Data
public class ApartmentDTO {

    /** 标题 */
    @NotBlank(message = "标题不能为空")
    private String title;

    /** 房源图片（逗号分隔URL列表，选填） */
    private String images;

    /** 户型 */
    private String layout;

    /** 面积（㎡） */
    @NotNull(message = "面积不能为空")
    private BigDecimal area;

    /** 月租金（元） */
    @NotNull(message = "租金不能为空")
    private Integer rent;

    /** 状态：可租 / 已租（新增时默认可租） */
    private String status;

    /** 配套设施（逗号分隔） */
    private String facilities;

    /** 位置信息 */
    private String address;
}
