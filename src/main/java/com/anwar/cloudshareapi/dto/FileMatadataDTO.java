package com.anwar.cloudshareapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileMatadataDTO {
    private String id;
    private String name;
    private String type;
    private Long size;
    private String clerkId;
    private Boolean isPublic;

    // fileLocation stores Cloudinary secure URL
    private String fileLocation;

    // Cloudinary-specific metadata
    private String cloudinaryPublicId;
    private String cloudinaryUrl;

    private LocalDateTime uploadedAt;
}
