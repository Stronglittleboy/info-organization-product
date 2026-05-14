package com.example.infoorg.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PendingEntryResponse {
    private String entryId;
    private String contentPreview;
    private String sourceType;
    private LocalDateTime capturedAt;
    private String currentSuggestedAction;
    private String insightText;
    private String topicId;
    private String topicName;
}
