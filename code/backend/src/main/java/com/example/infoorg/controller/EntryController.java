package com.example.infoorg.controller;

import com.example.infoorg.dto.request.CreateEntryRequest;
import com.example.infoorg.dto.request.UpdateInsightRequest;
import com.example.infoorg.dto.request.UpdateTopicRequest;
import com.example.infoorg.dto.response.EntryResponse;
import com.example.infoorg.dto.response.PageResponse;
import com.example.infoorg.dto.response.PendingEntryResponse;
import com.example.infoorg.service.EntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class EntryController {

    private final EntryService entryService;

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
            @RequestParam String keyword,
            @RequestParam(required = false) String topicId,
            @RequestParam(required = false) Boolean hasInsight,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return entryService.searchEntries(keyword, topicId, hasInsight, startDate, endDate, cursor, limit);
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
}
