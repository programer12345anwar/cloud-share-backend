package com.anwar.cloudshareapi.controller;

import com.anwar.cloudshareapi.document.UserCredits;
import com.anwar.cloudshareapi.dto.FileMatadataDTO;
import com.anwar.cloudshareapi.service.FileMatadataService;
import com.anwar.cloudshareapi.service.UserCreditsService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FileController handles file operations (upload, retrieve, delete)
 * Files are stored on Cloudinary cloud storage
 * Database stores metadata and Cloudinary URLs
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileController {
    private final FileMatadataService fileMatadataService;
    private final UserCreditsService userCreditsService;

    /**
     * Upload files to Cloudinary
     * 
     * @param files Array of files to upload
     * @return Upload response with file metadata and remaining credits
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFiles(@RequestPart("files") MultipartFile files[]) throws IOException {
        Map<String, Object> response = new HashMap<>();
        List<FileMatadataDTO> list = fileMatadataService.uploadFiles(files);

        UserCredits finalCredits = userCreditsService.getUserCredits();
        response.put("files", list);
        response.put("remainingCredits", finalCredits.getCredits());
        return ResponseEntity.ok(response);
    }

    /**
     * Get all files for current user
     */
    @GetMapping("/my")
    public ResponseEntity<?> getFilesForCurrentUser() {
        List<FileMatadataDTO> files = fileMatadataService.getFiles();
        return ResponseEntity.ok(files);
    }

    /**
     * Get a public file by ID
     */
    @GetMapping("/public/{id}")
    public ResponseEntity<?> getPublicFile(@PathVariable String id) {
        FileMatadataDTO file = fileMatadataService.getPublicFile(id);
        return ResponseEntity.ok(file);
    }

    /**
     * Download file from Cloudinary
     * Redirects to Cloudinary secure URL with download attachment header
     * Cloudinary handles CDN distribution and bandwidth
     * 
     * Returns HTTP 302 redirect to Cloudinary URL with fl_attachment flag
     * This forces browser to download the file instead of opening it
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<?> download(@PathVariable String id) throws IOException {
        FileMatadataDTO downloadableFile = fileMatadataService.getDownloadableFile(id);

        if (downloadableFile.getCloudinaryUrl() == null || downloadableFile.getCloudinaryUrl().isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "File URL not found"));
        }

        // Add Cloudinary download flag to force attachment download
        // Replace /upload/ with /upload/fl_attachment/ to add the attachment flag
        String downloadUrl = downloadableFile.getCloudinaryUrl()
                .replace("/upload/", "/upload/fl_attachment/");

        // Redirect to the Cloudinary URL with attachment flag
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", downloadUrl)
                .build();
    }

    /**
     * Delete a file
     * Removes from Cloudinary and database
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable String id) {
        fileMatadataService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggle file public/private visibility
     */
    @PatchMapping("/{id}/toggle-public")
    public ResponseEntity<?> togglePublic(@PathVariable String id) {
        FileMatadataDTO file = fileMatadataService.togglePublic(id);
        return ResponseEntity.ok(file);
    }
}
