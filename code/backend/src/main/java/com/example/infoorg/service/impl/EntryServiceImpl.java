package com.example.infoorg.service.impl;

import com.example.infoorg.dto.request.CreateEntryRequest;
import com.example.infoorg.dto.response.EntryResponse;
import com.example.infoorg.entity.Entry;
import com.example.infoorg.entity.Topic;
import com.example.infoorg.mapper.EntryMapper;
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
