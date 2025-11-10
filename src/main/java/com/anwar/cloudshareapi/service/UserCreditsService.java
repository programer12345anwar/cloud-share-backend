package com.anwar.cloudshareapi.service;

import com.anwar.cloudshareapi.document.UserCredits;
import com.anwar.cloudshareapi.repository.UserCreditsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCreditsService {
    private final UserCreditsRepository userCreditsRepository;
    public UserCredits createInitialCredits(String clerkId){
        UserCredits userCredits=UserCredits.builder()
                .clerkId(clerkId)
                .credits(5)
                .plan("BASIC")
                .build();
        return userCreditsRepository.save(userCredits);
    }
}
