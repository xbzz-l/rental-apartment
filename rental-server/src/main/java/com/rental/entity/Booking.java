package com.rental.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租房记录实体（对应 booking 表）
 */
@Data
@TableName("`booking`")
public class Booking {

    /** 记录ID（主键） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预约用户ID */
    private Long userId;

    /** 房源ID */
    private Long apartmentId;

    /** 记录状态：已预约 / 已成交 / 已取消 */
    private String status;

    /** 预约时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    // ---- 以下为列表展示用的冗余字段（不落库，由 Service 关联查询后填充） ----

    /** 房源标题 */
    @TableField(exist = false)
    private String apartmentTitle;

    /** 房源首图 */
    @TableField(exist = false)
    private String apartmentImage;

    /** 户型 */
    @TableField(exist = false)
    private String layout;

    /** 月租金 */
    @TableField(exist = false)
    private Integer rent;

    /** 预约用户账号 */
    @TableField(exist = false)
    private String username;
}
