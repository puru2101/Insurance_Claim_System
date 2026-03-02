package com.insurance.policy.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.data.domain.Page;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    @Builder.Default private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> success(String msg, T data) {
        return ApiResponse.<T>builder().success(true).message(msg).data(data).build();
    }
    public static <T> ApiResponse<T> success(String msg) {
        return ApiResponse.<T>builder().success(true).message(msg).build();
    }
}
