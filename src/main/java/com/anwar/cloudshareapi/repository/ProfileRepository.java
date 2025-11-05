package com.anwar.cloudshareapi.repository;


import com.anwar.cloudshareapi.document.ProfileDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ProfileRepository extends MongoRepository<ProfileDocument,String> {
    Optional<ProfileDocument>findByEmail(String email);
}
