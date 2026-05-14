package com.example.infoorg.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlMetadataResponse {
    
    /**
     * 网页标题
     */
    private String title;
    
    /**
     * 网页描述
     */
    private String description;
    
    /**
     * 网页正文（前 1000 字）
     */
    private String extractedText;
    
    /**
     * Favicon URL
     */
    private String favicon;
}
