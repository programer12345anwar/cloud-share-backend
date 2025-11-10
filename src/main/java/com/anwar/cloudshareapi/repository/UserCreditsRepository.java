package com.anwar.cloudshareapi.repository;

import com.anwar.cloudshareapi.document.UserCredits;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserCreditsRepository extends MongoRepository<UserCredits,String> {
}
