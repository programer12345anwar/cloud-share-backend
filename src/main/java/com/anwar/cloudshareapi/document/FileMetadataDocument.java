package com.anwar.cloudshareapi.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "files")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class FileMetadataDocument {
    @Id
    private String id;
    private String name;
    private String type;
    private Long size;
    private String clerkId;
    private Boolean isPublic;

    // fileLocation now stores Cloudinary secure URL instead of local path
    private String fileLocation;

    // Cloudinary-specific identifiers for file management
    private String cloudinaryPublicId; // Public ID needed for deletion and transformations
    private String cloudinaryUrl; // Direct HTTPS URL to the file

    private LocalDateTime uploadedAt;
}
