package com.example.infoorg.controller;

import com.example.infoorg.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class FileController {

    private static final Map<String, MediaType> EXT = Map.of(
            ".jpg", MediaType.IMAGE_JPEG,
            ".jpeg", MediaType.IMAGE_JPEG,
            ".png", MediaType.IMAGE_PNG,
            ".gif", MediaType.IMAGE_GIF,
            ".webp", new MediaType("image", "webp")
    );

    private final FileStorageService fileStorageService;

    @GetMapping("/files/{userId}/images/{year}/{month}/{filename}")
    public ResponseEntity<byte[]> getImage(@PathVariable String userId,
                                           @PathVariable String year,
                                           @PathVariable String month,
                                           @PathVariable String filename) throws Exception {
        return serve(userId, "images", year, month, filename);
    }

    @GetMapping("/files/{userId}/thumbnails/{year}/{month}/{filename}")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable String userId,
                                                 @PathVariable String year,
                                                 @PathVariable String month,
                                                 @PathVariable String filename) throws Exception {
        return serve(userId, "thumbnails", year, month, filename);
    }

    private ResponseEntity<byte[]> serve(String userId, String category, String year, String month, String filename) throws Exception {
        byte[] body = fileStorageService.readPublicObject(userId, category, year, month, filename);
        MediaType mediaType = guessType(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(mediaType)
                .body(body);
    }

    private static MediaType guessType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, MediaType> e : EXT.entrySet()) {
            if (lower.endsWith(e.getKey())) {
                return e.getValue();
            }
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
