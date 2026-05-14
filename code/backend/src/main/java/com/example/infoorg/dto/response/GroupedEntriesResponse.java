package com.example.infoorg.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GroupedEntriesResponse {

    private List<EntryResponse> today;
    private List<EntryResponse> yesterday;
    private List<EntryResponse> thisWeek;
    private List<EntryResponse> earlier;
}
