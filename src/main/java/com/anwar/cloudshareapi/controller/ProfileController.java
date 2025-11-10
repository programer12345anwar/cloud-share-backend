package com.anwar.cloudshareapi.controller;

import com.anwar.cloudshareapi.dto.ProfileDTO;
import com.anwar.cloudshareapi.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;

    @PostMapping("/register")
    public ResponseEntity<?> registerProfile(@RequestBody ProfileDTO profileDTO){
        HttpStatus status=profileService.existsByClerkId(profileDTO.getClerkId())?HttpStatus.OK:HttpStatus.CREATED;
        ProfileDTO saveResponse=profileService.createProfile(profileDTO);
        return ResponseEntity.status(status).body(saveResponse);
    }

}
