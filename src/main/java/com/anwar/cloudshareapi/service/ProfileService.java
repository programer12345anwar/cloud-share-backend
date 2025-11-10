package com.anwar.cloudshareapi.service;

import com.anwar.cloudshareapi.document.ProfileDocument;
import com.anwar.cloudshareapi.dto.ProfileDTO;
import com.anwar.cloudshareapi.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final ProfileRepository profileRepository;



    public ProfileDTO createProfile(ProfileDTO profileDTO){

        if(profileRepository.existsByClerkId(profileDTO.getClerkId())){
            return updateProfile(profileDTO);
        }
        ProfileDocument profile=ProfileDocument.builder()
                .clerkId(profileDTO.getClerkId())
                .email(profileDTO.getEmail())
                .firstName(profileDTO.getFirstName())
                .lastName(profileDTO.getLastName())
                .photoUrl(profileDTO.getPhotoUrl())
                .credits(5)
                .createdAt(Instant.now())
                .build();
        profile=profileRepository.save(profile);

        return ProfileDTO.builder()
                .id(profile.getId())
                .clerkId(profile.getClerkId())
                .email(profile.getEmail())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .photoUrl(profile.getPhotoUrl())
                .credits(profile.getCredits())
                .createdAt(profile.getCreatedAt())
                .build();
    }

    public ProfileDTO updateProfile(ProfileDTO profileDTO){
        ProfileDocument existingProfile=profileRepository.findByClerkId(profileDTO.getClerkId());
        if(existingProfile!=null){
            if(profileDTO.getEmail()!=null && !profileDTO.getEmail().isEmpty()){  //email = null, email = ""
                existingProfile.setEmail(profileDTO.getEmail());
            }
            if(profileDTO.getFirstName()!=null && !profileDTO.getFirstName().isEmpty()){
                existingProfile.setFirstName(profileDTO.getFirstName());
            }
            if(profileDTO.getLastName()!=null && !profileDTO.getLastName().isEmpty()){
                existingProfile.setLastName(profileDTO.getLastName());
            }
            if(profileDTO.getPhotoUrl()!=null && !profileDTO.getPhotoUrl().isEmpty()){
                existingProfile.setPhotoUrl(profileDTO.getPhotoUrl());
            }
            profileRepository.save(existingProfile);
            return ProfileDTO.builder()//Creates and returns a builder object.
                    .id(existingProfile.getId())
                    .email(existingProfile.getEmail())
                    .clerkId(existingProfile.getClerkId())
                    .firstName(existingProfile.getFirstName())
                    .lastName(existingProfile.getLastName())
                    .credits(existingProfile.getCredits())
                    .createdAt(existingProfile.getCreatedAt())
                    .photoUrl(existingProfile.getPhotoUrl())
                    .build();//constructs the ProfileDTO object.
        }
        return null;
    }

    public Boolean existsByClerkId(String clerkId){
        return profileRepository.existsByClerkId(clerkId);
    }

    public void deleteProfile(String clerkId) {
        ProfileDocument existingProfile = profileRepository.findByClerkId(clerkId);
        if (existingProfile != null) {
            profileRepository.delete(existingProfile);
        }
    }

}
