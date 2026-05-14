package com.example.infoorg.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "storage")
@Data
public class StorageConfig {
    
    private String mode = "local"; // "local" or "minio"
    
    private MinioProperties minio = new MinioProperties();
    private LocalProperties local = new LocalProperties();
    
    @Data
    public static class MinioProperties {
        private String endpoint = "http://192.168.31.173:9000";
        private String accessKey = "minioadmin";
        private String secretKey = "minioadmin123";
        private String bucket = "info-org-files";
        /** 浏览器访问对象的前缀，例如 http://192.168.31.173:9000 */
        private String publicUrl = "http://192.168.31.173:9000";
    }
    
    @Data
    public static class LocalProperties {
        private String basePath = "/vol3/1000/private/workProject/info-organization-product/data/uploads";
        /** 经后端代理访问文件时的 API 根，例如 http://192.168.31.173:8080/api */
        private String publicBaseUrl = "http://192.168.31.173:8080/api";
    }
    
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
    }
}
