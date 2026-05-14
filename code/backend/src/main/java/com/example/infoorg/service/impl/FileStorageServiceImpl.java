package com.example.infoorg.service.impl;

import com.example.infoorg.config.StorageConfig;
import com.example.infoorg.service.FileStorageService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private final StorageConfig storageConfig;
    private final MinioClient minioClient;

    @Override
    public String store(MultipartFile file, String userId, String category) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";

        LocalDate now = LocalDate.now();
        String year = now.format(DateTimeFormatter.ofPattern("yyyy"));
        String month = now.format(DateTimeFormatter.ofPattern("MM"));
        String filename = UUID.randomUUID() + extension;
        String safeCategory = StringUtils.hasText(category) ? category : "images";
        String objectPath = String.format("%s/%s/%s/%s/%s", userId, safeCategory, year, month, filename);

        if ("minio".equals(storageConfig.getMode())) {
            return storeToMinio(file, objectPath);
        }
        return storeToLocal(file, objectPath);
    }

    private String storeToMinio(MultipartFile file, String objectPath) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(storageConfig.getMinio().getBucket())
                        .object(objectPath)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

        String filePath = String.format("minio://%s/%s",
                storageConfig.getMinio().getBucket(), objectPath);
        log.info("File stored to MinIO: {}", filePath);
        return filePath;
    }

    private String storeToLocal(MultipartFile file, String objectPath) throws Exception {
        Path basePath = Paths.get(storageConfig.getLocal().getBasePath());
        Path targetPath = basePath.resolve(objectPath);

        Files.createDirectories(targetPath.getParent());
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        String filePath = targetPath.toAbsolutePath().normalize().toString();
        log.info("File stored to local: {}", filePath);
        return filePath;
    }

    @Override
    public InputStream getFile(String filePath) throws Exception {
        if (filePath.startsWith("minio://")) {
            return getFileFromMinio(filePath);
        }
        return getFileFromLocal(filePath);
    }

    private InputStream getFileFromMinio(String filePath) throws Exception {
        String path = filePath.substring("minio://".length());
        String bucket = path.substring(0, path.indexOf('/'));
        String objectPath = path.substring(path.indexOf('/') + 1);

        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectPath)
                        .build()
        );
    }

    private InputStream getFileFromLocal(String filePath) throws Exception {
        return new FileInputStream(new File(filePath));
    }

    @Override
    public void delete(String filePath) throws Exception {
        if (filePath.startsWith("minio://")) {
            deleteFromMinio(filePath);
        } else {
            deleteFromLocal(filePath);
        }
    }

    private void deleteFromMinio(String filePath) throws Exception {
        String path = filePath.substring("minio://".length());
        String bucket = path.substring(0, path.indexOf('/'));
        String objectPath = path.substring(path.indexOf('/') + 1);

        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectPath)
                        .build()
        );
        log.info("File deleted from MinIO: {}", filePath);
    }

    private void deleteFromLocal(String filePath) throws Exception {
        Files.deleteIfExists(Paths.get(filePath));
        log.info("File deleted from local: {}", filePath);
    }

    @Override
    public String getFileUrl(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return null;
        }
        if (filePath.startsWith("minio://")) {
            String path = filePath.substring("minio://".length());
            String base = storageConfig.getMinio().getPublicUrl().replaceAll("/+$", "");
            return base + "/" + path;
        }
        Path base = Paths.get(storageConfig.getLocal().getBasePath()).toAbsolutePath().normalize();
        Path absolute = Paths.get(filePath).toAbsolutePath().normalize();
        if (!absolute.startsWith(base)) {
            return null;
        }
        String relative = base.relativize(absolute).toString().replace(java.io.File.separatorChar, '/');
        String apiBase = storageConfig.getLocal().getPublicBaseUrl().replaceAll("/+$", "");
        return apiBase + "/files/" + relative;
    }

    @Override
    public byte[] readPublicObject(String userId, String category, String year, String month, String filename) throws Exception {
        validatePathParts(userId, category, year, month, filename);
        String objectPath = String.format("%s/%s/%s/%s/%s", userId, category, year, month, filename);
        if ("minio".equals(storageConfig.getMode())) {
            try (InputStream in = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(storageConfig.getMinio().getBucket())
                            .object(objectPath)
                            .build())) {
                return in.readAllBytes();
            }
        }
        Path path = Paths.get(storageConfig.getLocal().getBasePath()).resolve(objectPath).toAbsolutePath().normalize();
        Path base = Paths.get(storageConfig.getLocal().getBasePath()).toAbsolutePath().normalize();
        if (!path.startsWith(base)) {
            throw new IllegalArgumentException("非法路径");
        }
        return Files.readAllBytes(path);
    }

    private static void validatePathParts(String userId, String category, String year, String month, String filename) {
        Objects.requireNonNull(userId, "userId");
        if (!"images".equals(category) && !"thumbnails".equals(category)) {
            throw new IllegalArgumentException("非法 category");
        }
        if (!year.matches("\\d{4}") || !month.matches("\\d{2}")) {
            throw new IllegalArgumentException("非法日期路径");
        }
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("非法文件名");
        }
    }
}
