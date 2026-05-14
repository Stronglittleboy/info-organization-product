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
}
