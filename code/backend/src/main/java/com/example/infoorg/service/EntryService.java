package com.example.infoorg.service;

import com.example.infoorg.dto.request.CollectTextRequest;
import com.example.infoorg.dto.request.CollectUrlRequest;
import com.example.infoorg.dto.request.CreateEntryRequest;
import com.example.infoorg.dto.request.UpdateEntryRequest;
import com.example.infoorg.dto.response.EntryResponse;
import com.example.infoorg.dto.response.GroupedEntriesResponse;
import com.example.infoorg.dto.response.OcrTextResponse;
import com.example.infoorg.dto.response.PageResponse;
import com.example.infoorg.dto.response.PendingEntryResponse;
import com.example.infoorg.dto.response.UrlMetadataResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EntryService {
    EntryResponse createEntry(CreateEntryRequest request);

    List<EntryResponse> getRecentEntries();

    void updateInsight(String entryId, String insightText);

    void updateTopic(String entryId, String topicId);

    PageResponse<PendingEntryResponse> getPendingEntries(int offset, int limit);

    void skipEntry(String entryId);

    EntryResponse getEntryById(String entryId);

    PageResponse<EntryResponse> searchEntries(String keyword, String topicId, Boolean hasInsight, String startDate, String endDate, String cursor, int limit);

    int recordReuse(String entryId, String reuseType);

    PageResponse<EntryResponse> getEntriesByTopicId(String topicId, String cursor, int limit);

    void deleteEntry(String entryId);

    List<EntryResponse> getReviewEntries(int limit);

    EntryResponse uploadImageEntry(MultipartFile file, String insight, String sourceType, String topicId) throws Exception;

    OcrTextResponse ocrImage(MultipartFile file) throws Exception;

    EntryResponse collectUrl(CollectUrlRequest request) throws Exception;

    UrlMetadataResponse extractUrlMetadata(String url) throws Exception;

    EntryResponse collectText(CollectTextRequest request);

    PageResponse<EntryResponse> listEntries(int page, int size, String contentType, String topicId);

    GroupedEntriesResponse getGroupedEntries();

    EntryResponse updateEntry(String entryId, UpdateEntryRequest request);
}
