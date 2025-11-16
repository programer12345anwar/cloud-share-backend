package com.anwar.cloudshareapi.controller;

import com.anwar.cloudshareapi.document.UserCredits;
import com.anwar.cloudshareapi.dto.UserCreditsDTO;
import com.anwar.cloudshareapi.service.UserCreditsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserCreditsController {
    private final UserCreditsService userCreditsService;

    @GetMapping("/credits")
    public ResponseEntity<?> getUserCredits(){
        UserCredits userCredits=userCreditsService.getUserCredits();
        UserCreditsDTO response=UserCreditsDTO.builder()
                .credits(userCredits.getCredits())
                .plan(userCredits.getPlan())
                .build();
        return ResponseEntity.ok(response);
    }
}
