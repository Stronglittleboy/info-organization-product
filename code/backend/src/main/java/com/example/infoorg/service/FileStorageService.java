package com.example.infoorg.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 文件存储服务接口
 * 支持 MinIO 和本地文件系统两种存储方式
 */
public interface FileStorageService {
    
    /**
     * 存储文件
     * @param file 文件
     * @param userId 用户ID
     * @param contentType 内容类型（image/url/file）
     * @return 文件路径（minio://bucket/path 或 /local/path）
     */
    String store(MultipartFile file, String userId, String contentType) throws Exception;
    
    /**
     * 获取文件输入流
     * @param filePath 文件路径
     * @return 输入流
     */
    InputStream getFile(String filePath) throws Exception;
    
    /**
     * 删除文件
     * @param filePath 文件路径
     */
    void delete(String filePath) throws Exception;
    
    /**
     * 获取文件访问 URL
     * @param filePath 文件路径
     * @return 访问 URL
     */
    String getFileUrl(String filePath);

    /**
     * 按公开 URL 路径读取对象（userId/images|thumbnails/year/month/filename）
     */
    byte[] readPublicObject(String userId, String category, String year, String month, String filename) throws Exception;
}
