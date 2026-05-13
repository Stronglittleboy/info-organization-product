package com.example.infoorg.controller;

import com.example.infoorg.dto.request.CreateEntryRequest;
import com.example.infoorg.dto.response.EntryResponse;
import com.example.infoorg.service.EntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
