package com.insurance.user.dto.request;

import com.insurance.user.entity.UserProfile;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUpdateRequest {

    private UserProfile.AccountStatus accountStatus;

    @Size(max = 50)
    private String policyNumber;

    @Size(max = 50)
    private String nationalId;

    @Size(max = 300)
    private String adminNote;
}
