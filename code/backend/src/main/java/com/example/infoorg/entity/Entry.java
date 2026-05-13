package com.example.infoorg.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("entries")
public class Entry {
    @TableField("id")
    private String id;

    @TableField("user_id")
    private String userId;

    @TableField("raw_content")
    private String rawContent;

    @TableField("content_type")
    private String contentType;

    @TableField("source_type")
    private String sourceType;

    @TableField("source_title")
    private String sourceTitle;

    @TableField("source_link")
    private String sourceLink;

    @TableField("captured_at")
    private LocalDateTime capturedAt;

    @TableField("insight_text")
    private String insightText;

    @TableField("topic_id")
    private String topicId;

    @TableField(exist = false)
    private String topicName;

    @TableField("deleted")
    private Integer deleted;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
