package com.insurance.user.service.impl;

import com.insurance.user.dto.request.AdminUpdateRequest;
import com.insurance.user.dto.request.CreateProfileRequest;
import com.insurance.user.dto.request.UpdateProfileRequest;
import com.insurance.user.dto.response.PagedResponse;
import com.insurance.user.dto.response.UserDocumentResponse;
import com.insurance.user.dto.response.UserProfileResponse;
import com.insurance.user.entity.UserActivity;
import com.insurance.user.entity.UserDocument;
import com.insurance.user.entity.UserProfile;
import com.insurance.user.exception.DuplicateResourceException;
import com.insurance.user.exception.ResourceNotFoundException;
import com.insurance.user.exception.StorageException;
import com.insurance.user.mapper.UserProfileMapper;
import com.insurance.user.repository.UserActivityRepository;
import com.insurance.user.repository.UserDocumentRepository;
import com.insurance.user.repository.UserProfileRepository;
import com.insurance.user.service.KafkaPublisherService;
import com.insurance.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserProfileRepository profileRepository;
    private final UserDocumentRepository documentRepository;
    private final UserActivityRepository activityRepository;
    private final UserProfileMapper mapper;
    private final KafkaPublisherService kafkaPublisher;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.base-url:http://localhost:8082/files}")
    private String baseUrl;

    // ── Profile operations ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByAuthUserId(Long authUserId) {
        UserProfile profile = findProfileOrThrow(authUserId);
        saveActivity(authUserId, UserActivity.ActivityType.PROFILE_VIEWED, "Profile viewed");
        return mapper.toResponse(profile);
    }

    @Override
    @Transactional
    public UserProfileResponse createProfile(CreateProfileRequest request) {
        if (profileRepository.existsByAuthUserId(request.getAuthUserId())) {
            throw new DuplicateResourceException(
                "Profile already exists for user: " + request.getAuthUserId());
        }
        UserProfile profile = mapper.fromCreateRequest(request);
        profile.setAccountStatus(UserProfile.AccountStatus.ACTIVE);
        UserProfile saved = profileRepository.save(profile);
        log.info("Profile created for authUserId: {}", request.getAuthUserId());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long authUserId, UpdateProfileRequest request) {
        UserProfile profile = findProfileOrThrow(authUserId);
        mapper.updateFromRequest(request, profile);
        UserProfile saved = profileRepository.save(profile);

        saveActivity(authUserId, UserActivity.ActivityType.PROFILE_UPDATED, "Profile updated");
        kafkaPublisher.publishProfileUpdated(authUserId, profile.getEmail());

        log.info("Profile updated for authUserId: {}", authUserId);
        return mapper.toResponse(saved);
    }

    // ── Admin operations ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserProfileResponse> getAllUsers(String search, Pageable pageable) {
        Page<UserProfile> page;
        if (StringUtils.hasText(search)) {
            page = profileRepository.searchUsers(search, pageable);
        } else {
            page = profileRepository.findAll(pageable);
        }
        return PagedResponse.from(page.map(mapper::toResponse));
    }

    @Override
    @Transactional
    public UserProfileResponse adminUpdateUser(Long authUserId, AdminUpdateRequest request) {
        UserProfile profile = findProfileOrThrow(authUserId);

        if (request.getAccountStatus() != null) {
            profile.setAccountStatus(request.getAccountStatus());
        }
        if (StringUtils.hasText(request.getPolicyNumber())) {
            profile.setPolicyNumber(request.getPolicyNumber());
        }
        if (StringUtils.hasText(request.getNationalId())) {
            profile.setNationalId(request.getNationalId());
        }

        UserProfile saved = profileRepository.save(profile);
        log.info("Admin updated user profile: {}", authUserId);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deactivateUser(Long authUserId, Long adminId) {
        UserProfile profile = findProfileOrThrow(authUserId);
        profile.setAccountStatus(UserProfile.AccountStatus.DEACTIVATED);
        profileRepository.save(profile);

        saveActivity(authUserId, UserActivity.ActivityType.ACCOUNT_DEACTIVATED,
            "Account deactivated by admin: " + adminId);
        kafkaPublisher.publishUserDeactivated(authUserId, profile.getEmail());

        log.info("User {} deactivated by admin {}", authUserId, adminId);
    }

    // ── Document operations ───────────────────────────────────────────────────

    @Override
    @Transactional
    public UserDocumentResponse uploadDocument(Long authUserId, MultipartFile file,
                                               String documentType) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("File is empty or not provided");
        }

        validateFileType(file);

        String fileUrl = storeFile(file, authUserId);

        UserDocument document = UserDocument.builder()
            .authUserId(authUserId)
            .documentType(UserDocument.DocumentType.valueOf(documentType.toUpperCase()))
            .fileName(file.getOriginalFilename())
            .fileUrl(fileUrl)
            .fileSizeBytes(file.getSize())
            .mimeType(file.getContentType())
            .verificationStatus(UserDocument.VerificationStatus.PENDING)
            .build();

        UserDocument saved = documentRepository.save(document);
        saveActivity(authUserId, UserActivity.ActivityType.DOCUMENT_UPLOADED,
            "Document uploaded: " + documentType);

        log.info("Document uploaded for user {}: {}", authUserId, documentType);
        return mapper.toDocumentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDocumentResponse> getDocuments(Long authUserId) {
        return documentRepository.findByAuthUserIdOrderByUploadedAtDesc(authUserId)
            .stream().map(mapper::toDocumentResponse).toList();
    }

    @Override
    @Transactional
    public void deleteDocument(Long authUserId, Long documentId) {
        UserDocument doc = documentRepository.findByIdAndAuthUserId(documentId, authUserId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Document not found: " + documentId));
        documentRepository.delete(doc);
        log.info("Document {} deleted for user {}", documentId, authUserId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UserProfile findProfileOrThrow(Long authUserId) {
        return profileRepository.findByAuthUserId(authUserId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "User profile not found for userId: " + authUserId));
    }

    private void saveActivity(Long authUserId, UserActivity.ActivityType type, String description) {
        try {
            activityRepository.save(UserActivity.builder()
                .authUserId(authUserId)
                .activityType(type)
                .description(description)
                .build());
        } catch (Exception e) {
            log.error("Failed to save activity log: {}", e.getMessage());
        }
    }

    private void validateFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null ||
            (!contentType.startsWith("image/") &&
             !contentType.equals("application/pdf"))) {
            throw new StorageException("Only image and PDF files are allowed");
        }
        if (file.getSize() > 10 * 1024 * 1024) {  // 10MB
            throw new StorageException("File size must not exceed 10MB");
        }
    }

    private String storeFile(MultipartFile file, Long authUserId) {
        try {
            String extension = getExtension(file.getOriginalFilename());
            String newFilename = authUserId + "_" + UUID.randomUUID() + extension;
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            Path targetPath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return baseUrl + "/" + newFilename;
        } catch (IOException e) {
            throw new StorageException("Could not store file: " + e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return "";
    }
}
