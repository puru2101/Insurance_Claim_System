package com.insurance.claim.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddNoteRequest {

    @NotBlank(message = "Note content is required")
    @Size(min = 3, max = 2000)
    private String content;

    /** If true, only agents/admins can see this note */
    private boolean internal = false;
}
