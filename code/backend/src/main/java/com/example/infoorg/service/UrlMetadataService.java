package com.example.infoorg.service;

import com.example.infoorg.dto.response.UrlMetadataResponse;

/**
 * URL 元数据提取服务
 */
public interface UrlMetadataService {
    
    /**
     * 提取 URL 的元数据
     * @param url 网页 URL
     * @return 元数据（标题、描述、正文等）
     */
    UrlMetadataResponse extractMetadata(String url) throws Exception;
}
