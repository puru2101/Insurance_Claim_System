package com.insurance.claim.service;

import com.insurance.claim.dto.request.*;
import com.insurance.claim.dto.response.*;
import com.insurance.claim.entity.Claim;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public interface ClaimService {

    // Customer operations
    ClaimResponse submitClaim(Long customerId, SubmitClaimRequest request);
    PagedResponse<ClaimSummaryResponse> getCustomerClaims(Long customerId, Claim.ClaimStatus status, Pageable pageable);
    ClaimResponse getClaimByNumber(String claimNumber, Long requestingUserId, boolean isAgent);
    void withdrawClaim(Long claimId, Long customerId);

    // Agent / Admin operations
    ClaimResponse updateClaimStatus(Long claimId, UpdateClaimStatusRequest request, Long agentId, String agentRole);
    PagedResponse<ClaimSummaryResponse> searchClaims(Claim.ClaimStatus status, Claim.ClaimType claimType,
        Long customerId, Long agentId, String policyNumber,
        LocalDate fromDate, LocalDate toDate, Pageable pageable);
    ClaimResponse assignAgent(Long claimId, Long agentId);

    // Notes
    ClaimNoteResponse addNote(Long claimId, AddNoteRequest request, Long authorId, String authorRole);

    // Documents
    ClaimDocumentResponse uploadDocument(Long claimId, MultipartFile file,
        String documentType, String description, Long uploadedByUserId);
    void deleteDocument(Long claimId, Long documentId, Long requestingUserId);

    // Stats
    ClaimStatsResponse getClaimStats();
}
