package com.insurance.user.dto.request;

import com.insurance.user.entity.UserProfile;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {

    @Size(max = 50)
    private String firstName;

    @Size(max = 50)
    private String lastName;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number")
    private String phoneNumber;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private UserProfile.Gender gender;

    @Size(max = 200)
    private String addressLine1;

    @Size(max = 200)
    private String addressLine2;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    @Size(max = 20)
    private String postalCode;

    @Size(max = 100)
    private String country;

    @Size(max = 100)
    private String occupation;

    @Positive(message = "Annual income must be positive")
    private Double annualIncome;

    @Size(max = 10)
    private String preferredLanguage;

    private Boolean notificationEmail;
    private Boolean notificationSms;
}
