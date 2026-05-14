package com.example.infoorg.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopicResponse {
    private String topicId;
    private String name;
    private String description;
    private Integer entryCount;
}
