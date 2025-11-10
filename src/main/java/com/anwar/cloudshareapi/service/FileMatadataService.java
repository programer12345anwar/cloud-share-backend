package com.anwar.cloudshareapi.service;

import com.anwar.cloudshareapi.document.FileMetadataDocument;
import com.anwar.cloudshareapi.document.ProfileDocument;
import com.anwar.cloudshareapi.dto.FileMatadataDTO;
import com.anwar.cloudshareapi.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileMatadataService {
    private final ProfileService profileService;
    private final UserCreditsService userCreditsService;
    private final FileMetadataRepository fileMetadataRepository;

    public List<FileMatadataDTO> uploadFiles(MultipartFile files[]) throws IOException {
        List<FileMetadataDocument> savedFiles=new ArrayList<>();
        ProfileDocument currentProfile=profileService.getCurrentProfile();

        if(!userCreditsService.hasEnoughCredits(files.length)){
            throw new RuntimeException("Not enough credits to upload files. Please purchase more credits");
        }

        Path uploadPath=Paths.get("upload").toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        for(MultipartFile file:files){
            String fileName=UUID.randomUUID()+"."+ StringUtils.getFilename(file.getOriginalFilename());
            Path targetLocation=uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(),targetLocation, StandardCopyOption.REPLACE_EXISTING);

            FileMetadataDocument fileMetadata= FileMetadataDocument.builder()
                    .fileLocation(targetLocation.toString())
                    .name(file.getOriginalFilename())
                    .size(file.getSize())
                    .type(file.getContentType())
                    .clerkId(currentProfile.getClerkId())
                    .isPublic(false)
                    .uploadedAt(LocalDateTime.now())
                    .build();
            userCreditsService.consumeCredit();
            savedFiles.add(fileMetadataRepository.save(fileMetadata));
        }
        return savedFiles.stream().map(fileMetadataDocument -> mapToDTO(fileMetadataDocument))
                .collect(Collectors.toList());
    }

    private FileMatadataDTO mapToDTO(FileMetadataDocument fileMetadataDocument) {
        return FileMatadataDTO.builder()
                .id(fileMetadataDocument.getId())
                .fileLocation(fileMetadataDocument.getFileLocation())
                .name(fileMetadataDocument.getName())
                .size(fileMetadataDocument.getSize())
                .type(fileMetadataDocument.getType())
                .clerkId(fileMetadataDocument.getClerkId())
                .isPublic(fileMetadataDocument.getIsPublic())
                .uploadedAt(fileMetadataDocument.getUploadedAt())
                .build();
    }
}
