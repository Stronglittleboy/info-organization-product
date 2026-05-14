package com.example.infoorg.service.impl;

import com.example.infoorg.dto.response.PageResponse;
import com.example.infoorg.dto.response.TopicResponse;
import com.example.infoorg.entity.Topic;
import com.example.infoorg.mapper.TopicMapper;
import com.example.infoorg.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {

    private static final String DEFAULT_USER_ID = "00000000-0000-0000-0000-000000000001";
    private final TopicMapper topicMapper;

    @Override
    public PageResponse<TopicResponse> getTopicList(int offset, int limit) {
        List<Topic> topics = topicMapper.selectTopicList(DEFAULT_USER_ID, offset, limit);
        long total = topicMapper.countTopics(DEFAULT_USER_ID);

        List<TopicResponse> items = topics.stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<TopicResponse>builder()
                .items(items)
                .total(total)
                .hasMore(offset + limit < total)
                .build();
    }

    @Override
    public List<TopicResponse> searchTopics(String keyword, int limit) {
        return topicMapper.searchTopicsByName(DEFAULT_USER_ID, keyword, limit)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public TopicResponse createTopic(String name, String description) {
        Topic existing = topicMapper.findByUserIdAndName(DEFAULT_USER_ID, name.trim());
        if (existing != null) {
            return toResponse(existing);
        }

        Topic topic = new Topic();
        topic.setId(UUID.randomUUID().toString());
        topic.setUserId(DEFAULT_USER_ID);
        topic.setName(name.trim());
        topic.setDescription(description);
        topic.setDeleted(0);
        topicMapper.insertTopic(topic);
        return toResponse(topic);
    }

    private TopicResponse toResponse(Topic topic) {
        return TopicResponse.builder()
                .topicId(topic.getId())
                .name(topic.getName())
                .description(topic.getDescription())
                .entryCount(topic.getEntryCount() != null ? topic.getEntryCount() : 0)
                .build();
    }
}
