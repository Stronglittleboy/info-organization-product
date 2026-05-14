package com.example.infoorg.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateTopicRequest {
    @NotBlank
    private String topicId;
}
