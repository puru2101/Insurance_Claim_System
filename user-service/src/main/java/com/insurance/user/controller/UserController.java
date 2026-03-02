package com.insurance.user.controller;

import com.insurance.user.dto.request.AdminUpdateRequest;
import com.insurance.user.dto.request.UpdateProfileRequest;
import com.insurance.user.dto.response.*;
import com.insurance.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * UserController
 *
 * Base path: /users  (gateway: /api/users/**)
 *
 * CUSTOMER endpoints (self-service):
 *   GET    /users/me              → get own profile
 *   PUT    /users/me              → update own profile
 *   GET    /users/me/documents    → list own documents
 *   POST   /users/me/documents    → upload a document
 *   DELETE /users/me/documents/{id}
 *
 * ADMIN endpoints:
 *   GET    /users/admin           → list all users (paginated + search)
 *   GET    /users/admin/{userId}  → get any user profile
 *   PUT    /users/admin/{userId}  → update any user (status, policy number)
 *   DELETE /users/admin/{userId}  → deactivate user
 *
 * HOW WE GET authUserId:
 * The gateway injects X-Auth-User-Id after JWT validation.
 * The JwtAuthenticationFilter stores it in authentication.details.
 * We extract it with getAuthUserId(authentication).
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ── Customer: own profile ──────────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            Authentication authentication) {
        Long userId = getAuthUserId(authentication);
        UserProfileResponse profile = userService.getProfileByAuthUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved", profile));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        Long userId = getAuthUserId(authentication);
        UserProfileResponse profile = userService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", profile));
    }

    // ── Customer: documents ────────────────────────────────────────────────────

    @GetMapping("/me/documents")
    public ResponseEntity<ApiResponse<List<UserDocumentResponse>>> getMyDocuments(
            Authentication authentication) {
        Long userId = getAuthUserId(authentication);
        List<UserDocumentResponse> docs = userService.getDocuments(userId);
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved", docs));
    }

    /**
     * Upload a document.
     * documentType must match UserDocument.DocumentType enum values.
     * Supported: NATIONAL_ID, PASSPORT, DRIVING_LICENSE, PROOF_OF_ADDRESS, etc.
     */
    @PostMapping(value = "/me/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserDocumentResponse>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            Authentication authentication) {
        Long userId = getAuthUserId(authentication);
        UserDocumentResponse doc = userService.uploadDocument(userId, file, documentType);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Document uploaded successfully", doc));
    }

    @DeleteMapping("/me/documents/{documentId}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable Long documentId,
            Authentication authentication) {
        Long userId = getAuthUserId(authentication);
        userService.deleteDocument(userId, documentId);
        return ResponseEntity.ok(ApiResponse.success("Document deleted"));
    }

    // ── Admin endpoints ────────────────────────────────────────────────────────

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<UserProfileResponse>>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();

        PagedResponse<UserProfileResponse> users = userService.getAllUsers(
            search, PageRequest.of(page, size, sort));
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", users));
    }

    @GetMapping("/admin/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserById(
            @PathVariable Long userId) {
        UserProfileResponse profile = userService.getProfileByAuthUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved", profile));
    }

    @PutMapping("/admin/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<ApiResponse<UserProfileResponse>> adminUpdateUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateRequest request) {
        UserProfileResponse profile = userService.adminUpdateUser(userId, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", profile));
    }

    @DeleteMapping("/admin/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            @PathVariable Long userId,
            Authentication authentication) {
        Long adminId = getAuthUserId(authentication);
        userService.deactivateUser(userId, adminId);
        return ResponseEntity.ok(ApiResponse.success("User deactivated successfully"));
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    /**
     * Extract the authenticated user's ID from the Authentication object.
     * The JwtAuthenticationFilter stores it as authentication.details (String).
     */
    private Long getAuthUserId(Authentication authentication) {
        Object details = authentication.getDetails();
        if (details instanceof String detailsStr && !detailsStr.isEmpty()) {
            return Long.parseLong(detailsStr);
        }
        throw new RuntimeException("Unable to extract user ID from authentication context");
    }
}
