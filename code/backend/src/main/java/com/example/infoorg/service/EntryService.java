package com.example.infoorg.service;

import com.example.infoorg.dto.request.CreateEntryRequest;
import com.example.infoorg.dto.response.EntryResponse;

import java.util.List;

public interface EntryService {
    EntryResponse createEntry(CreateEntryRequest request);

    List<EntryResponse> getRecentEntries();
}
