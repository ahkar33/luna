package com.luna.common.controller;

import com.luna.common.dto.UploadResponse;
import com.luna.common.exception.BadRequestException;
import com.luna.common.service.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Tag(name = "Upload", description = "File upload operations")
@SecurityRequirement(name = "bearerAuth")
public class UploadController {

    private static final Set<String> ALLOWED_FOLDERS = Set.of("profiles", "posts");

    private final CloudinaryService cloudinaryService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Upload a file",
        description = "Upload an image or video to storage and get back a URL. " +
                      "Use the returned URL in subsequent create/update calls. " +
                      "Allowed folders: profiles, posts."
    )
    public ResponseEntity<UploadResponse> upload(
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "Storage folder", example = "posts")
            @RequestParam(value = "folder", defaultValue = "posts") String folder) {

        if (!ALLOWED_FOLDERS.contains(folder)) {
            throw new BadRequestException("Invalid folder. Allowed values: " + ALLOWED_FOLDERS);
        }

        String contentType = file.getContentType();
        String url;
        if (contentType != null && contentType.startsWith("video/")) {
            url = cloudinaryService.uploadVideo(file, folder);
        } else {
            url = cloudinaryService.uploadImage(file, folder);
        }

        return ResponseEntity.ok(new UploadResponse(url));
    }
}
