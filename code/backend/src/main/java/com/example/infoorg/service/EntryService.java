package com.example.infoorg.service;

import com.example.infoorg.dto.request.CreateEntryRequest;
import com.example.infoorg.dto.response.EntryResponse;
import com.example.infoorg.dto.response.PageResponse;
import com.example.infoorg.dto.response.PendingEntryResponse;

import java.util.List;

public interface EntryService {
    EntryResponse createEntry(CreateEntryRequest request);

    List<EntryResponse> getRecentEntries();

    void updateInsight(String entryId, String insightText);

    void updateTopic(String entryId, String topicId);

    PageResponse<PendingEntryResponse> getPendingEntries(int offset, int limit);

    void skipEntry(String entryId);

    EntryResponse getEntryById(String entryId);
}
