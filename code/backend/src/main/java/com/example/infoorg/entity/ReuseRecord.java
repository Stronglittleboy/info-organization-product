package com.example.infoorg.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reuse_records")
public class ReuseRecord {
    @TableField("id")
    private String id;

    @TableField("entry_id")
    private String entryId;

    @TableField("user_id")
    private String userId;

    @TableField("reuse_type")
    private String reuseType;

    @TableField("reused_at")
    private LocalDateTime reusedAt;
}
