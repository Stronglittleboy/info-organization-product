package com.example.infoorg.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EntryResponse {
    private String entryId;
    private String rawContent;
    private String contentType;
    private String sourceType;
    private String sourceTitle;
    private String sourceLink;
    private LocalDateTime capturedAt;
    private String insightText;
    private String topicId;
    private String topicName;
}
