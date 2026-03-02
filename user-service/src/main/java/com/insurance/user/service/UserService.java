package com.insurance.user.service;

import com.insurance.user.dto.request.AdminUpdateRequest;
import com.insurance.user.dto.request.CreateProfileRequest;
import com.insurance.user.dto.request.UpdateProfileRequest;
import com.insurance.user.dto.response.PagedResponse;
import com.insurance.user.dto.response.UserDocumentResponse;
import com.insurance.user.dto.response.UserProfileResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {

    // Profile CRUD
    UserProfileResponse getProfileByAuthUserId(Long authUserId);
    UserProfileResponse updateProfile(Long authUserId, UpdateProfileRequest request);
    UserProfileResponse createProfile(CreateProfileRequest request);

    // Admin operations
    PagedResponse<UserProfileResponse> getAllUsers(String search, Pageable pageable);
    UserProfileResponse adminUpdateUser(Long authUserId, AdminUpdateRequest request);
    void deactivateUser(Long authUserId, Long adminId);

    // Documents
    UserDocumentResponse uploadDocument(Long authUserId, MultipartFile file, String documentType);
    List<UserDocumentResponse> getDocuments(Long authUserId);
    void deleteDocument(Long authUserId, Long documentId);
}
