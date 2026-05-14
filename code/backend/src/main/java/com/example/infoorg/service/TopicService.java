package com.example.infoorg.service;

import com.example.infoorg.dto.response.PageResponse;
import com.example.infoorg.dto.response.TopicResponse;

import java.util.List;

public interface TopicService {
    PageResponse<TopicResponse> getTopicList(int offset, int limit);

    List<TopicResponse> searchTopics(String keyword, int limit);

    TopicResponse createTopic(String name, String description);

    TopicResponse getTopicById(String topicId);

    TopicResponse updateTopic(String topicId, String name, String description);

    void deleteTopic(String topicId);
}
