package com.insurance.user.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateProfileRequest {

    @NotNull(message = "Auth user ID is required")
    private Long authUserId;

    @NotBlank(message = "Email is required")
    @Email
    private String email;

    @NotBlank(message = "First name is required")
    @Size(max = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50)
    private String lastName;

    private String phoneNumber;
}
