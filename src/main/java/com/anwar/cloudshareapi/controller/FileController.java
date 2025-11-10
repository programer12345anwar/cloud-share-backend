package com.anwar.cloudshareapi.controller;

import com.anwar.cloudshareapi.document.UserCredits;
import com.anwar.cloudshareapi.dto.FileMatadataDTO;
import com.anwar.cloudshareapi.service.FileMatadataService;
import com.anwar.cloudshareapi.service.UserCreditsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
}
