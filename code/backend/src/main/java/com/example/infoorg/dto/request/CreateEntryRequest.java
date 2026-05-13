package com.example.infoorg.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateEntryRequest {
    @NotBlank
    private String rawContent;
    private String contentType;
    private String sourceType;
    private String sourceTitle;
    private String sourceLink;
    private String topicName;
}
