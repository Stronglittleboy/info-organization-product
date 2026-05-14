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

    // 图片字段
    @TableField("image_path")
    private String imagePath;

    @TableField("image_ocr_text")
    private String imageOcrText;

    // URL 字段
    @TableField("url")
    private String url;

    @TableField("url_title")
    private String urlTitle;

    @TableField("url_description")
    private String urlDescription;

    @TableField("url_extracted_text")
    private String urlExtractedText;

    @TableField("deleted")
    private Integer deleted;

    @TableField("version")
    private Integer version;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
