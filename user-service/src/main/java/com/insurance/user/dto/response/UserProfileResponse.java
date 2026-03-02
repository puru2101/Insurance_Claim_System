package com.insurance.user.dto.response;

import com.insurance.user.entity.UserProfile;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private Long authUserId;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private UserProfile.Gender gender;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String policyNumber;
    private String occupation;
    private Double annualIncome;
    private String profilePictureUrl;
    private String preferredLanguage;
    private UserProfile.AccountStatus accountStatus;
    private Boolean notificationEmail;
    private Boolean notificationSms;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
