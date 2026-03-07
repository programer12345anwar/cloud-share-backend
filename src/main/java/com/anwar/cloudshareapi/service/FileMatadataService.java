package com.anwar.cloudshareapi.service;

import com.anwar.cloudshareapi.document.FileMetadataDocument;
import com.anwar.cloudshareapi.document.ProfileDocument;
import com.anwar.cloudshareapi.dto.FileMatadataDTO;
import com.anwar.cloudshareapi.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * FileMatadataService handles file upload, retrieval, and deletion
 * Uses Cloudinary for cloud storage instead of local file storage
 * Industry-standard approach: URLs stored in database, files in CDN
 */
@Service
@RequiredArgsConstructor
public class FileMatadataService {
    private final ProfileService profileService;
    private final UserCreditsService userCreditsService;
    private final FileMetadataRepository fileMetadataRepository;
    private final CloudinaryService cloudinaryService; // Cloud storage service

    /**
     * Upload multiple files to Cloudinary
     * Stores metadata in MongoDB with Cloudinary URLs
     * 
     * @param files Array of files to upload
     * @return List of uploaded file metadata
     * @throws IOException if upload fails
     */
    public List<FileMatadataDTO> uploadFiles(MultipartFile files[]) throws IOException {
        List<FileMetadataDocument> savedFiles = new ArrayList<>();
        ProfileDocument currentProfile = profileService.getCurrentProfile();

        // Verify user has enough credits
        if (!userCreditsService.hasEnoughCredits(files.length)) {
            throw new RuntimeException("Not enough credits to upload files. Please purchase more credits");
        }

        // Upload each file to Cloudinary
        for (MultipartFile file : files) {
            try {
                // Upload to Cloudinary and get response
                Map<String, Object> uploadResponse = cloudinaryService.uploadFile(file, currentProfile.getClerkId());

                // Create metadata document with Cloudinary URLs
                FileMetadataDocument fileMetadata = FileMetadataDocument.builder()
                        .name(file.getOriginalFilename())
                        .size(file.getSize())
                        .type(file.getContentType())
                        .clerkId(currentProfile.getClerkId())
                        .isPublic(false)
                        .uploadedAt(LocalDateTime.now())
                        // Store Cloudinary information
                        .cloudinaryPublicId((String) uploadResponse.get("cloudinaryPublicId"))
                        .cloudinaryUrl((String) uploadResponse.get("cloudinaryUrl"))
                        .fileLocation((String) uploadResponse.get("cloudinaryUrl")) // URL for backward compatibility
                        .build();

                // Save metadata to MongoDB
                FileMetadataDocument savedFile = fileMetadataRepository.save(fileMetadata);
                
                // Consume 1 credit per file
                userCreditsService.consumeCredit();
                
                savedFiles.add(savedFile);

                System.out.println("✅ File saved to database - Name: " + file.getOriginalFilename());

            } catch (IOException e) {
                System.err.println("❌ Error uploading file: " + e.getMessage());
                throw e;
            }
        }

        return savedFiles.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Map document to DTO
     */
    private FileMatadataDTO mapToDTO(FileMetadataDocument fileMetadataDocument) {
        return FileMatadataDTO.builder()
                .id(fileMetadataDocument.getId())
                .fileLocation(fileMetadataDocument.getFileLocation())
                .name(fileMetadataDocument.getName())
                .size(fileMetadataDocument.getSize())
                .type(fileMetadataDocument.getType())
                .clerkId(fileMetadataDocument.getClerkId())
                .isPublic(fileMetadataDocument.getIsPublic())
                .cloudinaryPublicId(fileMetadataDocument.getCloudinaryPublicId())
                .cloudinaryUrl(fileMetadataDocument.getCloudinaryUrl())
                .uploadedAt(fileMetadataDocument.getUploadedAt())
                .build();
    }

    /**
     * Get all files for current user
     */
    public List<FileMatadataDTO> getFiles() {
        ProfileDocument currentProfile = profileService.getCurrentProfile();
        List<FileMetadataDocument> files = fileMetadataRepository.findByClerkId(currentProfile.getClerkId());
        return files.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a public file by ID
     */
    public FileMatadataDTO getPublicFile(String id) {
        Optional<FileMetadataDocument> fileOptional = fileMetadataRepository.findById(id);
        if (fileOptional.isEmpty() || !fileOptional.get().getIsPublic()) {
            throw new RuntimeException("Unable to get the file");
        }
        FileMetadataDocument document = fileOptional.get();
        return mapToDTO(document);
    }

    /**
     * Get downloadable file metadata
     */
    public FileMatadataDTO getDownloadableFile(String id) {
        FileMetadataDocument file = fileMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));
        return mapToDTO(file);
    }

    /**
     * Delete file from Cloudinary and database
     * Also deletes from database
     */
    public void deleteFile(String id) {
        try {
            ProfileDocument currentProfile = profileService.getCurrentProfile();

            // Fetch file metadata
            FileMetadataDocument file = fileMetadataRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("File not found"));

            // Verify file belongs to current user (security check)
            if (!file.getClerkId().equals(currentProfile.getClerkId())) {
                throw new RuntimeException("File does not belong to the current user");
            }

            // Delete from Cloudinary
            if (file.getCloudinaryPublicId() != null) {
                cloudinaryService.deleteFile(file.getCloudinaryPublicId());
            }

            // Delete from database
            fileMetadataRepository.deleteById(id);

            System.out.println("✅ File deleted - ID: " + id + ", Name: " + file.getName());

        } catch (Exception e) {
            throw new RuntimeException("Error deleting the file: " + e.getMessage(), e);
        }
    }

    /**
     * Toggle file public/private status
     */
    public FileMatadataDTO togglePublic(String id) {
        FileMetadataDocument file = fileMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));
        file.setIsPublic(!file.getIsPublic());
        fileMetadataRepository.save(file);
        return mapToDTO(file);
    }
}

