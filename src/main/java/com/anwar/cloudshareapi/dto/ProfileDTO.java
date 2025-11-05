package com.anwar.cloudshareapi.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ProfileDTO {
    private String id;
    private String clerkId;
    @Indexed(unique = true)
    private String email;
    private String firstName;
    private String lastName;
    private Integer credits;
    private String photoUrl;
    @CreatedDate
    private Instant createdAt;
}
