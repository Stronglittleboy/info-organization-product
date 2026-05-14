package com.example.infoorg.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExtractUrlRequest {

    @NotBlank
    private String url;
}
