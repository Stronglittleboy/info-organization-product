package com.example.infoorg.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * 图片 OCR 服务
 */
public interface ImageOcrService {
    
    /**
     * 从图片中提取文字
     * @param imageFile 图片文件
     * @return 提取的文字
     */
    String extractText(MultipartFile imageFile) throws Exception;
    
    /**
     * 从图片文件中提取文字
     * @param imageFile 图片文件
     * @return 提取的文字
     */
    String extractText(File imageFile) throws Exception;
}
