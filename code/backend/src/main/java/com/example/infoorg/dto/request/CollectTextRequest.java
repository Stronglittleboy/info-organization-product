package com.example.infoorg.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CollectTextRequest {

    @NotBlank
    @JsonProperty("rawContent")
    private String rawContent;

    /** 可选；与「仅正文」场景兼容 */
    @JsonProperty("insight")
    private String insight;

    private String sourceType;

    private String topicId;
}
