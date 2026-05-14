package com.example.infoorg.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CollectUrlRequest {

    @NotBlank
    private String url;

    @NotBlank
    @JsonProperty("insight")
    private String insight;

    private String sourceType;

    private String topicId;
}
