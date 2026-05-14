package com.example.infoorg.service.impl;

import com.example.infoorg.service.ImageOcrService;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Slf4j
public class ImageOcrServiceImpl implements ImageOcrService {
    
    private final Tesseract tesseract;
    
    public ImageOcrServiceImpl() {
        this.tesseract = new Tesseract();
        // 设置 Tesseract 数据路径（需要下载 tessdata）
        // tesseract.setDatapath("/usr/share/tesseract-ocr/4.00/tessdata");
        tesseract.setLanguage("chi_sim+eng"); // 中文简体 + 英文
    }
    
    @Override
    public String extractText(MultipartFile imageFile) throws Exception {
        // 创建临时文件
        Path tempFile = Files.createTempFile("ocr-", imageFile.getOriginalFilename());
        try {
            imageFile.transferTo(tempFile.toFile());
            return extractText(tempFile.toFile());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
    
    @Override
    public String extractText(File imageFile) throws Exception {
        try {
            String text = tesseract.doOCR(imageFile);
            log.info("OCR extracted {} characters from {}", text.length(), imageFile.getName());
            return text.trim();
        } catch (TesseractException e) {
            log.error("OCR failed for {}: {}", imageFile.getName(), e.getMessage());
            throw new Exception("OCR 识别失败: " + e.getMessage(), e);
        }
    }
}
