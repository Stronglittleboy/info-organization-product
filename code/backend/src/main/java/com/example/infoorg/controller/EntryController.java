package com.example.infoorg.controller;

import com.example.infoorg.dto.request.CollectTextRequest;
import com.example.infoorg.dto.request.CollectUrlRequest;
import com.example.infoorg.dto.request.CreateEntryRequest;
import com.example.infoorg.dto.request.ExtractUrlRequest;
import com.example.infoorg.dto.request.UpdateEntryRequest;
import com.example.infoorg.dto.request.UpdateInsightRequest;
import com.example.infoorg.dto.request.UpdateTopicRequest;
import com.example.infoorg.dto.response.EntryResponse;
import com.example.infoorg.dto.response.GroupedEntriesResponse;
import com.example.infoorg.dto.response.OcrTextResponse;
import com.example.infoorg.dto.response.PageResponse;
import com.example.infoorg.dto.response.PendingEntryResponse;
import com.example.infoorg.dto.response.UrlMetadataResponse;
import com.example.infoorg.service.EntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class EntryController {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_IMAGE_BYTES = 10 * 1024 * 1024L;

    private final EntryService entryService;

    @PostMapping("/entries/upload-image")
    public EntryResponse uploadImage(@RequestPart("file") MultipartFile file,
                                     @RequestParam("insight") String insight,
                                     @RequestParam(required = false) String sourceType,
                                     @RequestParam(required = false) String topicId) throws Exception {
        validateImageUpload(file);
        return entryService.uploadImageEntry(file, insight, sourceType, topicId);
    }

    @PostMapping("/entries/ocr")
    public OcrTextResponse ocrOnly(@RequestPart("file") MultipartFile file) throws Exception {
        validateImageUpload(file);
        return entryService.ocrImage(file);
    }

    @PostMapping("/entries/collect-url")
    public EntryResponse collectUrl(@Valid @RequestBody CollectUrlRequest request) throws Exception {
        return entryService.collectUrl(request);
    }

    @PostMapping("/entries/extract-url")
    public UrlMetadataResponse extractUrl(@Valid @RequestBody ExtractUrlRequest request) throws Exception {
        return entryService.extractUrlMetadata(request.getUrl());
    }

    @PostMapping("/entries/collect-text")
    public EntryResponse collectText(@Valid @RequestBody CollectTextRequest request) {
        return entryService.collectText(request);
    }

    @GetMapping("/entries")
    public PageResponse<EntryResponse> listEntries(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String topicId) {
        return entryService.listEntries(page, size, contentType, topicId);
    }

    @GetMapping("/entries/grouped")
    public GroupedEntriesResponse groupedEntries() {
        return entryService.getGroupedEntries();
    }

    @PostMapping("/entries")
    public EntryResponse createEntry(@Valid @RequestBody CreateEntryRequest request) {
        return entryService.createEntry(request);
    }

    @GetMapping("/entries/recent")
    public List<EntryResponse> getRecentEntries() {
        return entryService.getRecentEntries();
    }

    @GetMapping("/entries/{entryId}")
    public EntryResponse getEntry(@PathVariable String entryId) {
        return entryService.getEntryById(entryId);
    }

    @PutMapping("/entries/{entryId}")
    public EntryResponse putEntry(@PathVariable String entryId, @RequestBody UpdateEntryRequest request) {
        return entryService.updateEntry(entryId, request);
    }

    @PatchMapping("/entries/{entryId}/insight")
    public Map<String, Boolean> updateInsight(@PathVariable String entryId,
                                                @Valid @RequestBody UpdateInsightRequest request) {
        entryService.updateInsight(entryId, request.getInsightText());
        return Map.of("success", true);
    }

    @PatchMapping("/entries/{entryId}/topic")
    public Map<String, Boolean> updateTopic(@PathVariable String entryId,
                                              @Valid @RequestBody UpdateTopicRequest request) {
        entryService.updateTopic(entryId, request.getTopicId());
        return Map.of("success", true);
    }

    @GetMapping("/entries/pending")
    public PageResponse<PendingEntryResponse> getPendingEntries(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {
        return entryService.getPendingEntries(offset, limit);
    }

    @PostMapping("/entries/{entryId}/skip")
    public Map<String, Boolean> skipEntry(@PathVariable String entryId) {
        entryService.skipEntry(entryId);
        return Map.of("success", true);
    }

    @GetMapping("/entries/search")
    public PageResponse<EntryResponse> searchEntries(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String topicId,
            @RequestParam(required = false) Boolean hasInsight,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        String kw = StringUtils.hasText(q) ? q : keyword;
        if (!StringUtils.hasText(kw)) {
            throw new IllegalArgumentException("缺少搜索参数 q 或 keyword");
        }
        return entryService.searchEntries(kw, topicId, hasInsight, startDate, endDate, cursor, limit);
    }

    @PostMapping("/entries/{entryId}/reuse")
    public Map<String, Object> recordReuse(@PathVariable String entryId,
                                           @RequestBody Map<String, String> body) {
        String reuseType = body.getOrDefault("reuseType", "copy");
        int totalCount = entryService.recordReuse(entryId, reuseType);
        return Map.of("success", true, "totalCount", totalCount);
    }

    @GetMapping("/entries/review")
    public List<EntryResponse> getReviewEntries(@RequestParam(defaultValue = "5") int limit) {
        return entryService.getReviewEntries(limit);
    }

    @DeleteMapping("/entries/{entryId}")
    public Map<String, Boolean> deleteEntry(@PathVariable String entryId) {
        entryService.deleteEntry(entryId);
        return Map.of("success", true);
    }

    private void validateImageUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传图片文件");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("图片大小不能超过 10MB");
        }
        String ct = file.getContentType();
        if (ct == null || !ALLOWED_IMAGE_TYPES.contains(ct.toLowerCase())) {
            throw new IllegalArgumentException("仅支持 JPEG、PNG、WebP、GIF 图片");
        }
    }
}
