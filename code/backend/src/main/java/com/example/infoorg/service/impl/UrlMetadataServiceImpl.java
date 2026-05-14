package com.example.infoorg.service.impl;

import com.example.infoorg.dto.response.UrlMetadataResponse;
import com.example.infoorg.service.UrlMetadataService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UrlMetadataServiceImpl implements UrlMetadataService {
    
    private static final int TIMEOUT = 10000; // 10 秒超时
    private static final int MAX_TEXT_LENGTH = 1000; // 最多提取 1000 字
    
    @Override
    public UrlMetadataResponse extractMetadata(String url) throws Exception {
        try {
            Document doc = Jsoup.connect(url)
                    .timeout(TIMEOUT)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get();
            
            String title = extractTitle(doc);
            String description = extractDescription(doc);
            String extractedText = extractMainText(doc);
            String favicon = extractFavicon(doc, url);
            
            log.info("Extracted metadata from {}: title={}, description length={}, text length={}", 
                    url, title, description != null ? description.length() : 0, extractedText.length());
            
            return UrlMetadataResponse.builder()
                    .title(title)
                    .description(description)
                    .extractedText(extractedText)
                    .favicon(favicon)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to extract metadata from {}: {}", url, e.getMessage(), e);
            throw new Exception("无法提取网页元数据: " + e.getMessage(), e);
        }
    }
    
    private String extractTitle(Document doc) {
        // 优先级：og:title > title 标签
        Element ogTitle = doc.selectFirst("meta[property=og:title]");
        if (ogTitle != null && ogTitle.hasAttr("content")) {
            return ogTitle.attr("content");
        }
        
        return doc.title();
    }
    
    private String extractDescription(Document doc) {
        // 优先级：og:description > meta description
        Element ogDesc = doc.selectFirst("meta[property=og:description]");
        if (ogDesc != null && ogDesc.hasAttr("content")) {
            return ogDesc.attr("content");
        }
        
        Element metaDesc = doc.selectFirst("meta[name=description]");
        if (metaDesc != null && metaDesc.hasAttr("content")) {
            return metaDesc.attr("content");
        }
        
        return null;
    }
    
    private String extractMainText(Document doc) {
        // 移除脚本、样式、导航、页脚等
        doc.select("script, style, nav, footer, header, aside, .ad, .advertisement").remove();
        
        // 提取正文（优先 article, main, 或 body）
        Element main = doc.selectFirst("article, main, .content, .post, body");
        if (main == null) {
            main = doc.body();
        }
        
        String text = main.text();
        
        // 截取前 1000 字
        if (text.length() > MAX_TEXT_LENGTH) {
            text = text.substring(0, MAX_TEXT_LENGTH) + "...";
        }
        
        return text;
    }
    
    private String extractFavicon(Document doc, String baseUrl) {
        // 查找 favicon
        Element favicon = doc.selectFirst("link[rel~=icon]");
        if (favicon != null && favicon.hasAttr("href")) {
            String href = favicon.attr("abs:href");
            if (!href.isEmpty()) {
                return href;
            }
        }
        
        // 默认 /favicon.ico
        try {
            String domain = new java.net.URL(baseUrl).getProtocol() + "://" + 
                           new java.net.URL(baseUrl).getHost();
            return domain + "/favicon.ico";
        } catch (Exception e) {
            return null;
        }
    }
}
