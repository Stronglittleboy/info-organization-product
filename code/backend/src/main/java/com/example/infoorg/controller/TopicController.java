package com.example.infoorg.controller;

import com.example.infoorg.dto.response.PageResponse;
import com.example.infoorg.dto.response.TopicResponse;
import com.example.infoorg.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @GetMapping("/topics")
    public PageResponse<TopicResponse> getTopicList(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {
        return topicService.getTopicList(offset, limit);
    }

    @GetMapping("/topics/search")
    public List<TopicResponse> searchTopics(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        return topicService.searchTopics(keyword, limit);
    }

    @PostMapping("/topics")
    public TopicResponse createTopic(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String description = body.getOrDefault("description", null);
        if (name == null || name.isBlank()) {
            throw new RuntimeException("Topic name is required");
        }
        return topicService.createTopic(name, description);
    }
}
