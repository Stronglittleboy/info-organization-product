# 存储策略 - MinIO/本地双模式

## 设计目标

1. **灵活切换** - 支持 MinIO 和本地文件系统两种存储方式
2. **统一接口** - 上层业务代码无需关心底层存储实现
3. **易于扩展** - 未来可接入阿里云 OSS、AWS S3 等
4. **性能优化** - 支持缓存、CDN、缩略图等

---

## 存储模式对比

| 特性 | MinIO | 本地文件系统 |
|------|-------|-------------|
| **部署复杂度** | 需要 Docker 容器 | 无需额外部署 |
| **扩展性** | 支持分布式扩展 | 受限于单机磁盘 |
| **访问速度** | 网络 I/O | 本地 I/O（更快） |
| **备份** | 支持对象版本控制 | 需要手动备份 |
| **CDN 集成** | 容易（HTTP 访问） | 需要额外配置 |
| **成本** | 需要额外资源 | 无额外成本 |
| **推荐场景** | 生产环境、多实例 | 开发环境、单实例 |

---

## 存储路径设计

### MinIO 模式

```
Bucket: info-org-files

路径结构:
{userId}/
  ├── images/
  │   ├── 2026/
  │   │   ├── 05/
  │   │   │   ├── {uuid}.jpg
  │   │   │   ├── {uuid}.png
  │   │   │   └── ...
  │   │   └── 06/
  │   └── ...
  ├── thumbnails/          # 缩略图
  │   ├── 2026/
  │   │   └── 05/
  │   │       ├── {uuid}_thumb.jpg
  │   │       └── ...
  └── ...

存储路径示例:
minio://info-org-files/a1b2c3d4/images/2026/05/e5f6g7h8.jpg
```

### 本地文件系统模式

```
基础路径: /vol3/1000/private/workProject/info-organization-product/data/uploads

路径结构:
{userId}/
  ├── images/
  │   ├── 2026/
  │   │   ├── 05/
  │   │   │   ├── {uuid}.jpg
  │   │   │   └── ...
  │   │   └── 06/
  │   └── ...
  ├── thumbnails/
  │   └── ...
  └── ...

存储路径示例:
file:///vol3/1000/private/workProject/info-organization-product/data/uploads/a1b2c3d4/images/2026/05/e5f6g7h8.jpg
```

---

## 配置设计

### application.yml

```yaml
storage:
  # 存储模式: minio | local
  mode: minio
  
  # MinIO 配置
  minio:
    endpoint: http://192.168.31.173:9000
    access-key: minioadmin
    secret-key: minioadmin123
    bucket: info-org-files
    # 公开访问（用于前端直接访问图片）
    public-url: http://192.168.31.173:9000
  
  # 本地文件系统配置
  local:
    base-path: /vol3/1000/private/workProject/info-organization-product/data/uploads
    # 公开访问（通过后端代理）
    public-url: http://192.168.31.173:8080/api/files
  
  # 通用配置
  max-file-size: 104857600      # 100MB
  max-image-size: 10485760      # 10MB
  allowed-image-types:
    - image/jpeg
    - image/png
    - image/webp
    - image/gif
  
  # 缩略图配置
  thumbnail:
    enabled: true
    width: 400
    height: 400
    quality: 0.8
```

---

## 接口设计

### StorageService 接口

```java
public interface StorageService {
    /**
     * 保存文件
     * @param file 文件
     * @param userId 用户ID
     * @param category 分类（images/files/etc）
     * @return 存储路径（minio://... 或 file://...）
     */
    String save(MultipartFile file, String userId, String category) throws IOException;
    
    /**
     * 读取文件
     * @param path 存储路径
     * @return 文件字节流
     */
    byte[] read(String path) throws IOException;
    
    /**
     * 删除文件
     * @param path 存储路径
     */
    void delete(String path) throws IOException;
    
    /**
     * 获取公开访问 URL
     * @param path 存储路径
     * @return 公开访问 URL
     */
    String getPublicUrl(String path);
    
    /**
     * 生成缩略图
     * @param imagePath 原图路径
     * @return 缩略图路径
     */
    String generateThumbnail(String imagePath) throws IOException;
}
```

### MinIO 实现

```java
@Service
@ConditionalOnProperty(name = "storage.mode", havingValue = "minio")
public class MinioStorageService implements StorageService {
    
    @Autowired
    private MinioClient minioClient;
    
    @Value("${storage.minio.bucket}")
    private String bucket;
    
    @Value("${storage.minio.public-url}")
    private String publicUrl;
    
    @Override
    public String save(MultipartFile file, String userId, String category) throws IOException {
        // 生成路径: {userId}/{category}/{year}/{month}/{uuid}.{ext}
        String path = generatePath(userId, category, file.getOriginalFilename());
        
        // 上传到 MinIO
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucket)
                .object(path)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build()
        );
        
        return "minio://" + bucket + "/" + path;
    }
    
    @Override
    public String getPublicUrl(String path) {
        // minio://info-org-files/a1b2c3d4/images/2026/05/xxx.jpg
        // -> http://192.168.31.173:9000/info-org-files/a1b2c3d4/images/2026/05/xxx.jpg
        String objectPath = path.replace("minio://" + bucket + "/", "");
        return publicUrl + "/" + bucket + "/" + objectPath;
    }
}
```

### 本地文件系统实现

```java
@Service
@ConditionalOnProperty(name = "storage.mode", havingValue = "local")
public class LocalStorageService implements StorageService {
    
    @Value("${storage.local.base-path}")
    private String basePath;
    
    @Value("${storage.local.public-url}")
    private String publicUrl;
    
    @Override
    public String save(MultipartFile file, String userId, String category) throws IOException {
        // 生成路径
        String relativePath = generatePath(userId, category, file.getOriginalFilename());
        Path fullPath = Paths.get(basePath, relativePath);
        
        // 创建目录
        Files.createDirectories(fullPath.getParent());
        
        // 保存文件
        file.transferTo(fullPath.toFile());
        
        return "file://" + fullPath.toString();
    }
    
    @Override
    public String getPublicUrl(String path) {
        // file:///vol3/.../a1b2c3d4/images/2026/05/xxx.jpg
        // -> http://192.168.31.173:8080/api/files/a1b2c3d4/images/2026/05/xxx.jpg
        String relativePath = path.replace("file://" + basePath + "/", "");
        return publicUrl + "/" + relativePath;
    }
}
```

---

## 文件访问控制

### 权限验证

```java
@RestController
@RequestMapping("/api/files")
public class FileController {
    
    @Autowired
    private StorageService storageService;
    
    @Autowired
    private EntryRepository entryRepository;
    
    /**
     * 访问文件（需要权限验证）
     */
    @GetMapping("/{userId}/images/{year}/{month}/{filename}")
    public ResponseEntity<byte[]> getFile(
        @PathVariable String userId,
        @PathVariable String year,
        @PathVariable String month,
        @PathVariable String filename,
        @AuthenticationPrincipal User currentUser
    ) throws IOException {
        // 权限验证：只能访问自己的文件
        if (!currentUser.getId().equals(userId)) {
            throw new ForbiddenException("无权访问此文件");
        }
        
        // 构建路径
        String path = String.format("file://%s/%s/images/%s/%s/%s", 
            basePath, userId, year, month, filename);
        
        // 读取文件
        byte[] content = storageService.read(path);
        
        // 返回文件
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .body(content);
    }
}
```

---

## 缩略图生成

### Thumbnailator 实现

```java
@Service
public class ThumbnailService {
    
    @Autowired
    private StorageService storageService;
    
    @Value("${storage.thumbnail.width}")
    private int width;
    
    @Value("${storage.thumbnail.height}")
    private int height;
    
    @Value("${storage.thumbnail.quality}")
    private double quality;
    
    public String generateThumbnail(String imagePath) throws IOException {
        // 读取原图
        byte[] imageBytes = storageService.read(imagePath);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
        
        // 生成缩略图
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Thumbnails.of(inputStream)
            .size(width, height)
            .outputQuality(quality)
            .toOutputStream(outputStream);
        
        // 保存缩略图
        String thumbnailPath = imagePath.replace("/images/", "/thumbnails/")
            .replace(getExtension(imagePath), "_thumb.jpg");
        
        // 保存到存储
        storageService.save(outputStream.toByteArray(), thumbnailPath);
        
        return thumbnailPath;
    }
}
```

---

## 迁移方案

### 从本地迁移到 MinIO

```java
@Service
public class StorageMigrationService {
    
    public void migrateLocalToMinio() {
        // 1. 查询所有本地文件路径
        List<Entry> entries = entryRepository.findByImagePathStartingWith("file://");
        
        for (Entry entry : entries) {
            try {
                // 2. 读取本地文件
                byte[] content = localStorageService.read(entry.getImagePath());
                
                // 3. 上传到 MinIO
                String minioPath = minioStorageService.save(content, entry.getUserId(), "images");
                
                // 4. 更新数据库
                entry.setImagePath(minioPath);
                entryRepository.save(entry);
                
                // 5. 删除本地文件（可选）
                // localStorageService.delete(entry.getImagePath());
                
            } catch (Exception e) {
                log.error("迁移失败: {}", entry.getId(), e);
            }
        }
    }
}
```

---

## 性能优化

### 1. CDN 集成

```yaml
storage:
  cdn:
    enabled: true
    domain: https://cdn.example.com
```

```java
@Override
public String getPublicUrl(String path) {
    if (cdnEnabled) {
        return cdnDomain + "/" + extractPath(path);
    }
    return publicUrl + "/" + extractPath(path);
}
```

### 2. 缓存策略

```java
@Cacheable(value = "files", key = "#path")
public byte[] read(String path) throws IOException {
    // 实际读取文件
}
```

### 3. 异步上传

```java
@Async
public CompletableFuture<String> saveAsync(MultipartFile file, String userId, String category) {
    return CompletableFuture.completedFuture(save(file, userId, category));
}
```

---

## 监控与告警

### 1. 存储空间监控

```java
@Scheduled(cron = "0 0 * * * *")  // 每小时检查
public void checkStorageSpace() {
    if (storageMode.equals("local")) {
        Path path = Paths.get(basePath);
        long usableSpace = Files.getFileStore(path).getUsableSpace();
        long totalSpace = Files.getFileStore(path).getTotalSpace();
        double usagePercent = (1 - (double) usableSpace / totalSpace) * 100;
        
        if (usagePercent > 80) {
            log.warn("存储空间不足: {}%", usagePercent);
            // 发送告警
        }
    }
}
```

### 2. 上传失败监控

```java
@Aspect
@Component
public class StorageMonitorAspect {
    
    @AfterThrowing(pointcut = "execution(* StorageService.save(..))", throwing = "ex")
    public void logUploadFailure(JoinPoint joinPoint, Exception ex) {
        log.error("文件上传失败", ex);
        // 记录到监控系统
    }
}
```

---

## 安全考虑

### 1. 文件类型校验

```java
public void validateFileType(MultipartFile file) {
    // 检查 MIME 类型
    String contentType = file.getContentType();
    if (!allowedTypes.contains(contentType)) {
        throw new InvalidFileTypeException("不支持的文件类型");
    }
    
    // 检查文件头（防止伪造）
    byte[] header = new byte[8];
    file.getInputStream().read(header);
    if (!isValidImageHeader(header)) {
        throw new InvalidFileTypeException("文件头校验失败");
    }
}
```

### 2. 文件大小限制

```java
public void validateFileSize(MultipartFile file) {
    if (file.getSize() > maxFileSize) {
        throw new FileTooLargeException("文件大小超过限制");
    }
}
```

### 3. 路径遍历防护

```java
private String sanitizePath(String path) {
    // 移除 ../ 等危险字符
    return path.replaceAll("\\.\\./", "");
}
```

---

## 推荐配置

### 开发环境
```yaml
storage:
  mode: local
```

### 生产环境
```yaml
storage:
  mode: minio
  cdn:
    enabled: true
```
