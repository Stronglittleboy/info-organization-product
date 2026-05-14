package com.example.infoorg.service.impl;

import com.example.infoorg.config.OptimisticLockException;
import com.example.infoorg.config.ResourceNotFoundException;
import com.example.infoorg.dto.request.CollectTextRequest;
import com.example.infoorg.dto.request.CollectUrlRequest;
import com.example.infoorg.dto.request.CreateEntryRequest;
import com.example.infoorg.dto.request.UpdateEntryRequest;
import com.example.infoorg.dto.response.EntryResponse;
import com.example.infoorg.dto.response.GroupedEntriesResponse;
import com.example.infoorg.dto.response.OcrTextResponse;
import com.example.infoorg.dto.response.PageResponse;
import com.example.infoorg.dto.response.PendingEntryResponse;
import com.example.infoorg.dto.response.UrlMetadataResponse;
import com.example.infoorg.entity.Entry;
import com.example.infoorg.entity.Topic;
import com.example.infoorg.mapper.EntryMapper;
import com.example.infoorg.mapper.ReuseRecordMapper;
import com.example.infoorg.mapper.TopicMapper;
import com.example.infoorg.service.EntryService;
import com.example.infoorg.service.FileStorageService;
import com.example.infoorg.service.ImageOcrService;
import com.example.infoorg.service.UrlMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EntryServiceImpl implements EntryService {

    private static final String DEFAULT_USER_ID = "00000000-0000-0000-0000-000000000001";
    private final EntryMapper entryMapper;
    private final TopicMapper topicMapper;
    private final ReuseRecordMapper reuseRecordMapper;
    private final FileStorageService fileStorageService;
    private final ImageOcrService imageOcrService;
    private final UrlMetadataService urlMetadataService;

    @Override
    public EntryResponse createEntry(CreateEntryRequest request) {
        Topic topic = resolveTopic(request.getTopicName());

        Entry entry = new Entry();
        entry.setId(UUID.randomUUID().toString());
        entry.setUserId(DEFAULT_USER_ID);
        entry.setTopicId(topic == null ? null : topic.getId());
        entry.setTopicName(topic == null ? null : topic.getName());
        entry.setRawContent(request.getRawContent());
        entry.setContentType(request.getContentType() == null ? "text" : request.getContentType());
        entry.setSourceType(request.getSourceType());
        entry.setSourceTitle(request.getSourceTitle());
        entry.setSourceLink(request.getSourceLink());
        entry.setCapturedAt(LocalDateTime.now());
        entry.setDeleted(0);
        entry.setVersion(0);
        entryMapper.insertEntry(entry);
        return toResponse(entry);
    }

    @Override
    public List<EntryResponse> getRecentEntries() {
        return entryMapper.selectRecentEntries()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void updateInsight(String entryId, String insightText) {
        Entry entry = entryMapper.selectById(entryId);
        if (entry == null) {
            throw new RuntimeException("Entry not found: " + entryId);
        }
        int rows = entryMapper.updateInsightText(entryId, insightText, entry.getVersion());
        if (rows == 0) {
            throw new OptimisticLockException("数据已被其他操作修改，请刷新后重试");
        }
    }

    @Override
    public void updateTopic(String entryId, String topicId) {
        Topic topic = topicMapper.selectById(topicId);
        if (topic == null) {
            throw new RuntimeException("Topic not found: " + topicId);
        }
        Entry entry = entryMapper.selectById(entryId);
        if (entry == null) {
            throw new RuntimeException("Entry not found: " + entryId);
        }
        int rows = entryMapper.updateTopicId(entryId, topicId, entry.getVersion());
        if (rows == 0) {
            throw new OptimisticLockException("数据已被其他操作修改，请刷新后重试");
        }
    }

    @Override
    public PageResponse<PendingEntryResponse> getPendingEntries(int offset, int limit) {
        List<Entry> entries = entryMapper.selectPendingEntries(DEFAULT_USER_ID, offset, limit);
        long total = entryMapper.countPendingEntries(DEFAULT_USER_ID);

        List<PendingEntryResponse> items = entries.stream()
                .map(this::toPendingResponse)
                .toList();

        return PageResponse.<PendingEntryResponse>builder()
                .items(items)
                .total(total)
                .hasMore(offset + limit < total)
                .build();
    }

    @Override
    public void skipEntry(String entryId) {
        int rows = entryMapper.skipEntry(entryId);
        if (rows == 0) {
            throw new RuntimeException("Entry not found: " + entryId);
        }
    }

    @Override
    public EntryResponse getEntryById(String entryId) {
        Entry entry = entryMapper.selectById(entryId);
        if (entry == null) {
            throw new RuntimeException("Entry not found: " + entryId);
        }
        EntryResponse resp = toResponse(entry);
        resp.setReusedCount(reuseRecordMapper.countByEntryId(entryId));
        resp.setLastReusedAt(reuseRecordMapper.lastReusedAt(entryId));
        return resp;
    }

    @Override
    public PageResponse<EntryResponse> searchEntries(String keyword, String topicId, Boolean hasInsight, String startDate, String endDate, String cursor, int limit) {
        List<Entry> entries = entryMapper.searchEntries(DEFAULT_USER_ID, keyword, topicId, hasInsight, startDate, endDate, cursor, limit + 1);
        long total = entryMapper.countSearchEntries(DEFAULT_USER_ID, keyword, topicId, hasInsight, startDate, endDate);

        boolean hasMore = entries.size() > limit;
        if (hasMore) {
            entries = entries.subList(0, limit);
        }

        String nextCursor = null;
        if (hasMore && !entries.isEmpty()) {
            Entry last = entries.get(entries.size() - 1);
            nextCursor = last.getCapturedAt() != null ? last.getCapturedAt().toString() : null;
        }

        List<EntryResponse> items = entries.stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<EntryResponse>builder()
                .items(items)
                .total(total)
                .hasMore(hasMore)
                .nextCursor(nextCursor)
                .build();
    }

    @Override
    public int recordReuse(String entryId, String reuseType) {
        Entry entry = entryMapper.selectById(entryId);
        if (entry == null) {
            throw new RuntimeException("Entry not found: " + entryId);
        }
        reuseRecordMapper.insertReuseRecord(UUID.randomUUID().toString(), entryId, DEFAULT_USER_ID, reuseType);
        return reuseRecordMapper.countByEntryId(entryId);
    }

    @Override
    public PageResponse<EntryResponse> getEntriesByTopicId(String topicId, String cursor, int limit) {
        List<Entry> entries = entryMapper.selectByTopicId(topicId, cursor, limit + 1);
        long total = entryMapper.countByTopicId(topicId);

        boolean hasMore = entries.size() > limit;
        if (hasMore) {
            entries = entries.subList(0, limit);
        }

        String nextCursor = null;
        if (hasMore && !entries.isEmpty()) {
            Entry last = entries.get(entries.size() - 1);
            nextCursor = last.getCapturedAt() != null ? last.getCapturedAt().toString() : null;
        }

        List<EntryResponse> items = entries.stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<EntryResponse>builder()
                .items(items)
                .total(total)
                .hasMore(hasMore)
                .nextCursor(nextCursor)
                .build();
    }

    @Override
    public void deleteEntry(String entryId) {
        int rows = entryMapper.softDelete(entryId);
        if (rows == 0) {
            throw new RuntimeException("Entry not found: " + entryId);
        }
    }

    @Override
    public List<EntryResponse> getReviewEntries(int limit) {
        return entryMapper.selectReviewEntries(DEFAULT_USER_ID, limit)
                .stream()
                .map(e -> {
                    EntryResponse resp = toResponse(e);
                    resp.setReusedCount(reuseRecordMapper.countByEntryId(e.getId()));
                    resp.setLastReusedAt(reuseRecordMapper.lastReusedAt(e.getId()));
                    return resp;
                })
                .toList();
    }

    @Override
    public EntryResponse uploadImageEntry(MultipartFile file, String insight, String sourceType, String topicId) throws Exception {
        String ocrText = imageOcrService.extractText(file);
        String storedPath = fileStorageService.store(file, DEFAULT_USER_ID, "images");

        Topic topic = resolveTopicById(topicId);
        Entry entry = new Entry();
        entry.setId(UUID.randomUUID().toString());
        entry.setUserId(DEFAULT_USER_ID);
        entry.setTopicId(topic == null ? null : topic.getId());
        entry.setTopicName(topic == null ? null : topic.getName());
        entry.setRawContent(StringUtils.hasText(ocrText) ? ocrText : "（图片）");
        entry.setContentType("image");
        entry.setSourceType(sourceType);
        entry.setInsightText(insight);
        entry.setImagePath(storedPath);
        entry.setImageOcrText(ocrText);
        entry.setCapturedAt(LocalDateTime.now());
        entry.setDeleted(0);
        entry.setVersion(0);
        entryMapper.insertEntry(entry);
        return toResponse(entry);
    }

    @Override
    public OcrTextResponse ocrImage(MultipartFile file) throws Exception {
        return new OcrTextResponse(imageOcrService.extractText(file));
    }

    @Override
    public EntryResponse collectUrl(CollectUrlRequest request) throws Exception {
        UrlMetadataResponse meta = urlMetadataService.extractMetadata(request.getUrl().trim());
        Topic topic = resolveTopicById(request.getTopicId());

        String body = meta.getExtractedText();
        if (!StringUtils.hasText(body)) {
            body = StringUtils.hasText(meta.getDescription()) ? meta.getDescription() : request.getUrl();
        }

        Entry entry = new Entry();
        entry.setId(UUID.randomUUID().toString());
        entry.setUserId(DEFAULT_USER_ID);
        entry.setTopicId(topic == null ? null : topic.getId());
        entry.setTopicName(topic == null ? null : topic.getName());
        entry.setRawContent(body);
        entry.setContentType("url");
        entry.setSourceType(request.getSourceType());
        entry.setInsightText(request.getInsight());
        entry.setUrl(request.getUrl().trim());
        entry.setUrlTitle(meta.getTitle());
        entry.setUrlDescription(meta.getDescription());
        entry.setUrlExtractedText(meta.getExtractedText());
        entry.setCapturedAt(LocalDateTime.now());
        entry.setDeleted(0);
        entry.setVersion(0);
        entryMapper.insertEntry(entry);
        return toResponse(entry);
    }

    @Override
    public UrlMetadataResponse extractUrlMetadata(String url) throws Exception {
        return urlMetadataService.extractMetadata(url.trim());
    }

    @Override
    public EntryResponse collectText(CollectTextRequest request) {
        Topic topic = resolveTopicById(request.getTopicId());
        Entry entry = new Entry();
        entry.setId(UUID.randomUUID().toString());
        entry.setUserId(DEFAULT_USER_ID);
        entry.setTopicId(topic == null ? null : topic.getId());
        entry.setTopicName(topic == null ? null : topic.getName());
        entry.setRawContent(request.getRawContent());
        entry.setContentType("text");
        entry.setSourceType(request.getSourceType());
        entry.setInsightText(StringUtils.hasText(request.getInsight()) ? request.getInsight().trim() : null);
        entry.setCapturedAt(LocalDateTime.now());
        entry.setDeleted(0);
        entry.setVersion(0);
        entryMapper.insertEntry(entry);
        return toResponse(entry);
    }

    @Override
    public PageResponse<EntryResponse> listEntries(int page, int size, String contentType, String topicId) {
        int p = Math.max(page, 1);
        int s = size < 1 ? 20 : Math.min(size, 100);
        int offset = (p - 1) * s;
        long total = entryMapper.countEntriesPage(DEFAULT_USER_ID, contentType, topicId);
        List<Entry> rows = entryMapper.selectEntriesPage(DEFAULT_USER_ID, contentType, topicId, offset, s);
        List<EntryResponse> items = rows.stream().map(this::toResponse).toList();
        return PageResponse.<EntryResponse>builder()
                .items(items)
                .total(total)
                .hasMore(offset + s < total)
                .build();
    }

    @Override
    public GroupedEntriesResponse getGroupedEntries() {
        List<Entry> rows = entryMapper.selectEntriesRecentForUser(DEFAULT_USER_ID, 500);
        LocalDate today = LocalDate.now();
        LocalDateTime weekStart = today.atStartOfDay().minusDays(7);

        List<EntryResponse> todayList = new ArrayList<>();
        List<EntryResponse> yesterdayList = new ArrayList<>();
        List<EntryResponse> weekList = new ArrayList<>();
        List<EntryResponse> earlierList = new ArrayList<>();

        for (Entry e : rows) {
            if (e.getCapturedAt() == null) {
                continue;
            }
            LocalDate d = e.getCapturedAt().toLocalDate();
            EntryResponse r = toResponse(e);
            if (d.equals(today)) {
                todayList.add(r);
            } else if (d.equals(today.minusDays(1))) {
                yesterdayList.add(r);
            } else if (!e.getCapturedAt().isBefore(weekStart)) {
                weekList.add(r);
            } else {
                earlierList.add(r);
            }
        }

        return GroupedEntriesResponse.builder()
                .today(todayList)
                .yesterday(yesterdayList)
                .thisWeek(weekList)
                .earlier(earlierList)
                .build();
    }

    @Override
    public EntryResponse updateEntry(String entryId, UpdateEntryRequest request) {
        Entry existing = entryMapper.selectById(entryId);
        if (existing == null) {
            throw new ResourceNotFoundException("Entry not found: " + entryId);
        }
        Entry patch = new Entry();
        patch.setId(entryId);
        patch.setVersion(existing.getVersion());
        if (request.getRawContent() != null) {
            patch.setRawContent(request.getRawContent());
        }
        if (request.getInsightText() != null) {
            patch.setInsightText(request.getInsightText());
        }
        if (request.getTopicId() != null) {
            if (!StringUtils.hasText(request.getTopicId())) {
                throw new IllegalArgumentException("topicId 不能为空字符串");
            }
            Topic topic = topicMapper.selectById(request.getTopicId());
            if (topic == null) {
                throw new ResourceNotFoundException("Topic not found: " + request.getTopicId());
            }
            patch.setTopicId(topic.getId());
        }
        if (request.getSourceType() != null) {
            patch.setSourceType(request.getSourceType());
        }
        if (request.getSourceTitle() != null) {
            patch.setSourceTitle(request.getSourceTitle());
        }
        if (request.getSourceLink() != null) {
            patch.setSourceLink(request.getSourceLink());
        }
        boolean any = request.getRawContent() != null
                || request.getInsightText() != null
                || request.getTopicId() != null
                || request.getSourceType() != null
                || request.getSourceTitle() != null
                || request.getSourceLink() != null;
        if (!any) {
            return getEntryById(entryId);
        }
        int rows = entryMapper.updateEntrySelective(patch);
        if (rows == 0) {
            throw new OptimisticLockException("数据已被其他操作修改，请刷新后重试");
        }
        return getEntryById(entryId);
    }

    private Topic resolveTopicById(String topicId) {
        if (!StringUtils.hasText(topicId)) {
            return null;
        }
        Topic topic = topicMapper.selectById(topicId.trim());
        if (topic == null) {
            throw new ResourceNotFoundException("Topic not found: " + topicId);
        }
        return topic;
    }

    private Topic resolveTopic(String topicName) {
        if (!StringUtils.hasText(topicName)) {
            return null;
        }
        Topic existing = topicMapper.findByUserIdAndName(DEFAULT_USER_ID, topicName.trim());
        if (existing != null) {
            return existing;
        }
        Topic topic = new Topic();
        topic.setId(UUID.randomUUID().toString());
        topic.setUserId(DEFAULT_USER_ID);
        topic.setName(topicName.trim());
        topic.setDeleted(0);
        topicMapper.insertTopic(topic);
        return topic;
    }

    private String deriveSuggestedAction(Entry entry) {
        if (entry.getInsightText() == null || entry.getInsightText().isBlank()) {
            return "ADD_INSIGHT";
        }
        if (entry.getTopicId() == null) {
            return "ADD_TOPIC";
        }
        return null;
    }

    private String contentPreview(String rawContent) {
        if (rawContent == null) return "";
        return rawContent.length() > 200 ? rawContent.substring(0, 200) + "..." : rawContent;
    }

    private PendingEntryResponse toPendingResponse(Entry entry) {
        return PendingEntryResponse.builder()
                .entryId(entry.getId())
                .contentPreview(contentPreview(entry.getRawContent()))
                .sourceType(entry.getSourceType())
                .capturedAt(entry.getCapturedAt())
                .currentSuggestedAction(deriveSuggestedAction(entry))
                .insightText(entry.getInsightText())
                .topicId(entry.getTopicId())
                .topicName(entry.getTopicName())
                .build();
    }

    private EntryResponse toResponse(Entry entry) {
        String thumb = null;
        if ("image".equals(entry.getContentType()) && StringUtils.hasText(entry.getImagePath())) {
            thumb = fileStorageService.getFileUrl(entry.getImagePath());
        }
        return EntryResponse.builder()
                .entryId(entry.getId())
                .rawContent(entry.getRawContent())
                .contentType(entry.getContentType())
                .sourceType(entry.getSourceType())
                .sourceTitle(entry.getSourceTitle())
                .sourceLink(entry.getSourceLink())
                .capturedAt(entry.getCapturedAt())
                .insightText(entry.getInsightText())
                .topicId(entry.getTopicId())
                .topicName(entry.getTopicName())
                .imagePath(entry.getImagePath())
                .imageOcrText(entry.getImageOcrText())
                .url(entry.getUrl())
                .urlTitle(entry.getUrlTitle())
                .urlDescription(entry.getUrlDescription())
                .thumbnailUrl(thumb)
                .build();
    }
}
