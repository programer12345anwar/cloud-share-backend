package com.anwar.cloudshareapi.controller;

import com.anwar.cloudshareapi.document.UserCredits;
import com.anwar.cloudshareapi.dto.FileMatadataDTO;
import com.anwar.cloudshareapi.service.FileMatadataService;
import com.anwar.cloudshareapi.service.UserCreditsService;
import lombok.RequiredArgsConstructor;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/files")

public class FileController {
    private final FileMatadataService fileMatadataService;
    private final UserCreditsService userCreditsService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFiles(@RequestPart("files")MultipartFile files[]) throws IOException {

        Map<String,Object> response=new HashMap<>();
        List<FileMatadataDTO> list=fileMatadataService.uploadFiles(files);

        UserCredits finalCredits=userCreditsService.getUserCredits();
        response.put("files",list);
        response.put("remainingCredits",finalCredits.getCredits());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<?> getFilesForCurrentUser(){
       List<FileMatadataDTO> files=fileMatadataService.getFiles();
       return ResponseEntity.ok(files);
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<?> getPublicFile(@PathVariable String id){
        FileMatadataDTO file=fileMatadataService.getPublicFile(id);
        return ResponseEntity.ok(file);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable String id) throws IOException {
        FileMatadataDTO downloadableFile = fileMatadataService.getDownloadableFile(id);

        // Convert path to a Resource
        java.nio.file.Path path = Paths.get(downloadableFile.getFileLocation());
        org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(path);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadableFile.getName() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable String id){
        fileMatadataService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-public")
    public ResponseEntity<?> togglePublic(@PathVariable String id){
        FileMatadataDTO file=fileMatadataService.togglePublic(id);
        return ResponseEntity.ok(file);
    }

}
