package com.luna.common.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.luna.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {
    
    private final Cloudinary cloudinary;
    
    // Free tier limits
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_VIDEO_SIZE = 40 * 1024 * 1024; // 40MB
    private static final String[] ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/png", "image/jpg", "image/webp"};
    private static final String[] ALLOWED_VIDEO_TYPES = {"video/mp4", "video/webm", "video/quicktime"};
    
    public String uploadImage(MultipartFile file, String folder) {
        validateImage(file);
        return uploadFile(file, folder, "image");
    }
    
    public String uploadVideo(MultipartFile file, String folder) {
        validateVideo(file);
        return uploadFile(file, folder, "video");
    }
    
    public void deleteFile(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Deleted file from Cloudinary: {}", publicId);
        } catch (IOException e) {
            log.error("Failed to delete file from Cloudinary: {}", publicId, e);
            // Don't throw exception, just log the error
        }
    }
    
    private String uploadFile(MultipartFile file, String folder, String resourceType) {
        try {
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                "folder", folder,
                "resource_type", resourceType
            );
            
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            log.error("Failed to upload file to Cloudinary", e);
            throw new BadRequestException("Failed to upload file: " + e.getMessage());
        }
    }
    
    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BadRequestException("Image size must not exceed 10MB");
        }
        
        String contentType = file.getContentType();
        boolean isValidType = false;
        for (String allowedType : ALLOWED_IMAGE_TYPES) {
            if (allowedType.equals(contentType)) {
                isValidType = true;
                break;
            }
        }
        
        if (!isValidType) {
            throw new BadRequestException("Invalid image type. Allowed types: JPEG, PNG, JPG, WEBP");
        }
    }
    
    private void validateVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Video file is required");
        }
        
        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new BadRequestException("Video size must not exceed 40MB");
        }
        
        String contentType = file.getContentType();
        boolean isValidType = false;
        for (String allowedType : ALLOWED_VIDEO_TYPES) {
            if (allowedType.equals(contentType)) {
                isValidType = true;
                break;
            }
        }
        
        if (!isValidType) {
            throw new BadRequestException("Invalid video type. Allowed types: MP4, WEBM, MOV");
        }
    }
    
    public String extractPublicId(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        try {
            // Extract public_id from Cloudinary URL
            // Format: https://res.cloudinary.com/{cloud_name}/{resource_type}/upload/{version}/{public_id}.{format}
            String[] parts = url.split("/upload/");
            if (parts.length < 2) {
                return null;
            }
            
            String afterUpload = parts[1];
            // Remove version if present (v1234567890/)
            if (afterUpload.matches("v\\d+/.*")) {
                afterUpload = afterUpload.substring(afterUpload.indexOf('/') + 1);
            }
            
            // Remove file extension
            int lastDot = afterUpload.lastIndexOf('.');
            if (lastDot > 0) {
                afterUpload = afterUpload.substring(0, lastDot);
            }
            
            return afterUpload;
        } catch (Exception e) {
            log.error("Failed to extract public_id from URL: {}", url, e);
            return null;
        }
    }
}
