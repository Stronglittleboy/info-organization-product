package com.example.infoorg.dto.request;

import lombok.Data;

@Data
public class UpdateEntryRequest {

    private String rawContent;
    private String insightText;
    private String topicId;
    private String sourceType;
    private String sourceTitle;
    private String sourceLink;
}
