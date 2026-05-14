package com.example.infoorg.service.impl;

import com.example.infoorg.dto.request.CreateEntryRequest;
import com.example.infoorg.dto.response.EntryResponse;
import com.example.infoorg.dto.response.PageResponse;
import com.example.infoorg.dto.response.PendingEntryResponse;
import com.example.infoorg.entity.Entry;
import com.example.infoorg.entity.Topic;
import com.example.infoorg.mapper.EntryMapper;
import com.example.infoorg.mapper.ReuseRecordMapper;
import com.example.infoorg.mapper.TopicMapper;
import com.example.infoorg.service.EntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EntryServiceImpl implements EntryService {

    private static final String DEFAULT_USER_ID = "00000000-0000-0000-0000-000000000001";
    private final EntryMapper entryMapper;
    private final TopicMapper topicMapper;
    private final ReuseRecordMapper reuseRecordMapper;

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
        int rows = entryMapper.updateInsightText(entryId, insightText);
        if (rows == 0) {
            throw new RuntimeException("Entry not found: " + entryId);
        }
    }

    @Override
    public void updateTopic(String entryId, String topicId) {
        Topic topic = topicMapper.selectById(topicId);
        if (topic == null) {
            throw new RuntimeException("Topic not found: " + topicId);
        }
        int rows = entryMapper.updateTopicId(entryId, topicId);
        if (rows == 0) {
            throw new RuntimeException("Entry not found: " + entryId);
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
        return toResponse(entry);
    }

    @Override
    public PageResponse<EntryResponse> searchEntries(String keyword, String topicId, Boolean hasInsight, int offset, int limit) {
        List<Entry> entries = entryMapper.searchEntries(DEFAULT_USER_ID, keyword, topicId, hasInsight, offset, limit);
        long total = entryMapper.countSearchEntries(DEFAULT_USER_ID, keyword, topicId, hasInsight);

        List<EntryResponse> items = entries.stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<EntryResponse>builder()
                .items(items)
                .total(total)
                .hasMore(offset + limit < total)
                .build();
    }

    @Override
    public void recordReuse(String entryId, String reuseType) {
        Entry entry = entryMapper.selectById(entryId);
        if (entry == null) {
            throw new RuntimeException("Entry not found: " + entryId);
        }
        reuseRecordMapper.insertReuseRecord(UUID.randomUUID().toString(), entryId, DEFAULT_USER_ID, reuseType);
    }

    @Override
    public PageResponse<EntryResponse> getEntriesByTopicId(String topicId, int offset, int limit) {
        List<Entry> entries = entryMapper.selectByTopicId(topicId, offset, limit);
        long total = entryMapper.countByTopicId(topicId);

        List<EntryResponse> items = entries.stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<EntryResponse>builder()
                .items(items)
                .total(total)
                .hasMore(offset + limit < total)
                .build();
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
                .build();
    }
}
