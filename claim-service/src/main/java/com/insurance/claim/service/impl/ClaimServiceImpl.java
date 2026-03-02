package com.insurance.claim.service.impl;

import com.insurance.claim.dto.request.*;
import com.insurance.claim.dto.response.*;
import com.insurance.claim.entity.*;
import com.insurance.claim.event.ClaimEvent;
import com.insurance.claim.exception.*;
import com.insurance.claim.exception.AccessDeniedException;
import com.insurance.claim.mapper.ClaimMapper;
import com.insurance.claim.repository.*;
import com.insurance.claim.service.ClaimService;
import com.insurance.claim.service.KafkaPublisherService;
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
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimServiceImpl implements ClaimService {

    private final ClaimRepository           claimRepository;
    private final ClaimDocumentRepository   documentRepository;
    private final ClaimNoteRepository       noteRepository;
    private final ClaimStatusHistoryRepository historyRepository;
    private final ClaimMapper               mapper;
    private final KafkaPublisherService     kafkaPublisher;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.base-url:http://localhost:8083/files}")
    private String baseUrl;

    // ── Submit ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ClaimResponse submitClaim(Long customerId, SubmitClaimRequest request) {
        String claimNumber = generateClaimNumber();

        Claim claim = Claim.builder()
            .claimNumber(claimNumber)
            .customerId(customerId)
            .policyNumber(request.getPolicyNumber())
            .claimType(request.getClaimType())
            .status(Claim.ClaimStatus.SUBMITTED)
            .incidentDate(request.getIncidentDate())
            .incidentDescription(request.getIncidentDescription())
            .incidentLocation(request.getIncidentLocation())
            .claimedAmount(request.getClaimedAmount())
            .priority(calculatePriority(request.getClaimedAmount()))
            .build();

        Claim saved = claimRepository.save(claim);

        // Record initial status history
        recordStatusChange(saved, null, Claim.ClaimStatus.SUBMITTED, customerId, "CUSTOMER", "Claim submitted");

        // Publish Kafka event (async)
        kafkaPublisher.publishClaimEvent(buildEvent("CLAIM_SUBMITTED", saved, null,
            Claim.ClaimStatus.SUBMITTED, customerId, "CUSTOMER", null));

        log.info("Claim submitted: {} by customer: {}", claimNumber, customerId);
        return buildFullResponse(saved, true);
    }

    // ── Get customer claims ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ClaimSummaryResponse> getCustomerClaims(
            Long customerId, Claim.ClaimStatus status, Pageable pageable) {

        Page<Claim> page = (status != null)
            ? claimRepository.findByCustomerIdAndStatusOrderBySubmittedAtDesc(customerId, status, pageable)
            : claimRepository.findByCustomerIdOrderBySubmittedAtDesc(customerId, pageable);

        return PagedResponse.from(page.map(mapper::toSummary));
    }

    // ── Get claim by number ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ClaimResponse getClaimByNumber(String claimNumber, Long requestingUserId, boolean isAgent) {
        Claim claim = claimRepository.findByClaimNumber(claimNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Claim not found: " + claimNumber));

        // Customers can only view their own claims
        if (!isAgent && !claim.getCustomerId().equals(requestingUserId)) {
            throw new AccessDeniedException("You don't have access to this claim");
        }

        return buildFullResponse(claim, isAgent);
    }

    // ── Withdraw ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void withdrawClaim(Long claimId, Long customerId) {
        Claim claim = claimRepository.findById(claimId)
            .orElseThrow(() -> new ResourceNotFoundException("Claim not found: " + claimId));

        if (!claim.getCustomerId().equals(customerId)) {
            throw new AccessDeniedException("You don't have access to this claim");
        }

        Set<Claim.ClaimStatus> withdrawableStatuses = Set.of(
            Claim.ClaimStatus.SUBMITTED, Claim.ClaimStatus.PENDING_INFO);

        if (!withdrawableStatuses.contains(claim.getStatus())) {
            throw new InvalidClaimOperationException(
                "Cannot withdraw claim in status: " + claim.getStatus() +
                ". Only SUBMITTED or PENDING_INFO claims can be withdrawn.");
        }

        Claim.ClaimStatus prev = claim.getStatus();
        claim.setStatus(Claim.ClaimStatus.WITHDRAWN);
        claim.setResolvedAt(LocalDateTime.now());
        claimRepository.save(claim);

        recordStatusChange(claim, prev, Claim.ClaimStatus.WITHDRAWN, customerId, "CUSTOMER", "Withdrawn by customer");
        kafkaPublisher.publishClaimEvent(buildEvent("CLAIM_WITHDRAWN", claim, prev,
            Claim.ClaimStatus.WITHDRAWN, customerId, "CUSTOMER", null));

        log.info("Claim {} withdrawn by customer {}", claim.getClaimNumber(), customerId);
    }

    // ── Update status (agent/admin) ────────────────────────────────────────────

    @Override
    @Transactional
    public ClaimResponse updateClaimStatus(Long claimId, UpdateClaimStatusRequest request,
                                           Long agentId, String agentRole) {
        Claim claim = claimRepository.findById(claimId)
            .orElseThrow(() -> new ResourceNotFoundException("Claim not found: " + claimId));

        validateStatusTransition(claim.getStatus(), request.getNewStatus());

        Claim.ClaimStatus prev = claim.getStatus();
        claim.setStatus(request.getNewStatus());

        // Apply status-specific fields
        switch (request.getNewStatus()) {
            case UNDER_REVIEW -> {
                claim.setReviewStartedAt(LocalDateTime.now());
            }
            case APPROVED -> {
                if (request.getApprovedAmount() == null)
                    throw new InvalidClaimOperationException("Approved amount is required when approving a claim");
                claim.setApprovedAmount(request.getApprovedAmount());
                claim.setResolvedAt(LocalDateTime.now());
            }
            case REJECTED -> {
                if (!StringUtils.hasText(request.getRejectionReason()))
                    throw new InvalidClaimOperationException("Rejection reason is required when rejecting a claim");
                claim.setRejectionReason(request.getRejectionReason());
                claim.setResolvedAt(LocalDateTime.now());
            }
            case SETTLED -> {
                if (request.getSettledAmount() == null)
                    throw new InvalidClaimOperationException("Settled amount is required when settling a claim");
                claim.setSettledAmount(request.getSettledAmount());
                claim.setSettlementNotes(request.getSettlementNotes());
                claim.setSettledAt(LocalDateTime.now());
                claim.setResolvedAt(LocalDateTime.now());
            }
            default -> { /* PENDING_INFO, etc. — no extra fields required */ }
        }

        claimRepository.save(claim);
        recordStatusChange(claim, prev, request.getNewStatus(), agentId, agentRole, request.getComment());

        kafkaPublisher.publishClaimEvent(buildEvent(
            "STATUS_CHANGED", claim, prev, request.getNewStatus(),
            agentId, agentRole, request.getComment()));

        log.info("Claim {} status changed: {} → {} by agent {}",
            claim.getClaimNumber(), prev, request.getNewStatus(), agentId);
        return buildFullResponse(claim, true);
    }

    // ── Search (admin/agent) ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ClaimSummaryResponse> searchClaims(
            Claim.ClaimStatus status, Claim.ClaimType claimType,
            Long customerId, Long agentId, String policyNumber,
            LocalDate fromDate, LocalDate toDate, Pageable pageable) {

        Page<Claim> page = claimRepository.searchClaims(
            status, claimType, customerId, agentId, policyNumber, fromDate, toDate, pageable);
        return PagedResponse.from(page.map(mapper::toSummary));
    }

    // ── Assign agent ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ClaimResponse assignAgent(Long claimId, Long agentId) {
        Claim claim = claimRepository.findById(claimId)
            .orElseThrow(() -> new ResourceNotFoundException("Claim not found: " + claimId));

        claim.setAssignedAgentId(agentId);
        if (claim.getStatus() == Claim.ClaimStatus.SUBMITTED) {
            claim.setStatus(Claim.ClaimStatus.UNDER_REVIEW);
            claim.setReviewStartedAt(LocalDateTime.now());
            recordStatusChange(claim, Claim.ClaimStatus.SUBMITTED,
                Claim.ClaimStatus.UNDER_REVIEW, agentId, "AGENT", "Agent assigned, review started");
        }
        claimRepository.save(claim);

        kafkaPublisher.publishClaimEvent(buildEvent("AGENT_ASSIGNED", claim,
            null, claim.getStatus(), agentId, "AGENT", "Agent assigned: " + agentId));

        log.info("Agent {} assigned to claim {}", agentId, claim.getClaimNumber());
        return buildFullResponse(claim, true);
    }

    // ── Notes ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ClaimNoteResponse addNote(Long claimId, AddNoteRequest request,
                                     Long authorId, String authorRole) {
        Claim claim = claimRepository.findById(claimId)
            .orElseThrow(() -> new ResourceNotFoundException("Claim not found: " + claimId));

        ClaimNote note = ClaimNote.builder()
            .claim(claim)
            .authorUserId(authorId)
            .authorRole(authorRole)
            .content(request.getContent())
            .isInternal(request.isInternal())
            .build();

        return mapper.toNoteResponse(noteRepository.save(note));
    }

    // ── Documents ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ClaimDocumentResponse uploadDocument(Long claimId, MultipartFile file,
                                                String documentType, String description,
                                                Long uploadedByUserId) {
        Claim claim = claimRepository.findById(claimId)
            .orElseThrow(() -> new ResourceNotFoundException("Claim not found: " + claimId));

        validateFile(file);
        String fileUrl = storeFile(file, claimId);

        ClaimDocument doc = ClaimDocument.builder()
            .claim(claim)
            .uploadedByUserId(uploadedByUserId)
            .documentType(ClaimDocument.DocumentType.valueOf(documentType.toUpperCase()))
            .fileName(file.getOriginalFilename())
            .fileUrl(fileUrl)
            .fileSizeBytes(file.getSize())
            .mimeType(file.getContentType())
            .description(description)
            .build();

        log.info("Document uploaded for claim {}: {}", claim.getClaimNumber(), documentType);
        return mapper.toDocumentResponse(documentRepository.save(doc));
    }

    @Override
    @Transactional
    public void deleteDocument(Long claimId, Long documentId, Long requestingUserId) {
        ClaimDocument doc = documentRepository.findByIdAndClaimId(documentId, claimId)
            .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        documentRepository.delete(doc);
    }

    // ── Stats ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ClaimStatsResponse getClaimStats() {
        long total    = claimRepository.count();
        long approved = claimRepository.countByStatus(Claim.ClaimStatus.APPROVED);
        long settled  = claimRepository.countByStatus(Claim.ClaimStatus.SETTLED);
        long rejected = claimRepository.countByStatus(Claim.ClaimStatus.REJECTED);

        Map<String, Long> byType = claimRepository.countByClaimType().stream()
            .collect(Collectors.toMap(
                row -> ((Claim.ClaimType) row[0]).name(),
                row -> (Long) row[1]));

        double approvalRate = total > 0 ? (double)(approved + settled) / total * 100 : 0;
        Double avgDays = claimRepository.avgResolutionDays();

        return ClaimStatsResponse.builder()
            .totalClaims(total)
            .submittedCount(claimRepository.countByStatus(Claim.ClaimStatus.SUBMITTED))
            .underReviewCount(claimRepository.countByStatus(Claim.ClaimStatus.UNDER_REVIEW))
            .pendingInfoCount(claimRepository.countByStatus(Claim.ClaimStatus.PENDING_INFO))
            .approvedCount(approved)
            .rejectedCount(rejected)
            .settledCount(settled)
            .totalClaimedAmount(claimRepository.sumTotalClaimedAmount())
            .totalApprovedAmount(claimRepository.sumTotalApprovedAmount())
            .totalSettledAmount(claimRepository.sumTotalSettledAmount())
            .claimsByType(byType)
            .approvalRate(Math.round(approvalRate * 10.0) / 10.0)
            .avgProcessingDays(avgDays != null ? Math.round(avgDays * 10.0) / 10.0 : 0)
            .build();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * State machine — defines which transitions are valid.
     *
     *  SUBMITTED     → UNDER_REVIEW, WITHDRAWN
     *  UNDER_REVIEW  → APPROVED, REJECTED, PENDING_INFO
     *  PENDING_INFO  → UNDER_REVIEW, WITHDRAWN
     *  APPROVED      → SETTLED
     *  REJECTED      → (terminal)
     *  SETTLED       → (terminal)
     *  WITHDRAWN     → (terminal)
     */
    private void validateStatusTransition(Claim.ClaimStatus from, Claim.ClaimStatus to) {
        Map<Claim.ClaimStatus, Set<Claim.ClaimStatus>> allowed = Map.of(
            Claim.ClaimStatus.SUBMITTED,    Set.of(Claim.ClaimStatus.UNDER_REVIEW, Claim.ClaimStatus.WITHDRAWN),
            Claim.ClaimStatus.UNDER_REVIEW, Set.of(Claim.ClaimStatus.APPROVED, Claim.ClaimStatus.REJECTED, Claim.ClaimStatus.PENDING_INFO),
            Claim.ClaimStatus.PENDING_INFO, Set.of(Claim.ClaimStatus.UNDER_REVIEW, Claim.ClaimStatus.WITHDRAWN),
            Claim.ClaimStatus.APPROVED,     Set.of(Claim.ClaimStatus.SETTLED)
        );

        Set<Claim.ClaimStatus> validNext = allowed.getOrDefault(from, Set.of());
        if (!validNext.contains(to)) {
            throw new InvalidClaimOperationException(
                String.format("Invalid status transition: %s → %s. Allowed: %s", from, to, validNext));
        }
    }

    private void recordStatusChange(Claim claim, Claim.ClaimStatus from,
                                    Claim.ClaimStatus to, Long userId,
                                    String role, String comment) {
        historyRepository.save(ClaimStatusHistory.builder()
            .claim(claim)
            .fromStatus(from)
            .toStatus(to)
            .changedByUserId(userId)
            .changedByRole(role)
            .comment(comment)
            .build());
    }

    private ClaimResponse buildFullResponse(Claim claim, boolean includeInternalNotes) {
        ClaimResponse response = mapper.toResponse(claim);
        response.setNotes(mapper.toNoteResponseList(
            noteRepository.findByClaimId(claim.getId(), includeInternalNotes)));
        response.setStatusHistory(mapper.toHistoryResponseList(
            historyRepository.findByClaimIdOrderByChangedAtDesc(claim.getId())));
        return response;
    }

    private ClaimEvent buildEvent(String eventType, Claim claim,
                                  Claim.ClaimStatus prev, Claim.ClaimStatus next,
                                  Long userId, String role, String comment) {
        return ClaimEvent.builder()
            .eventType(eventType)
            .claimId(claim.getId())
            .claimNumber(claim.getClaimNumber())
            .customerId(claim.getCustomerId())
            .agentId(claim.getAssignedAgentId())
            .policyNumber(claim.getPolicyNumber())
            .claimType(claim.getClaimType())
            .previousStatus(prev)
            .newStatus(next)
            .claimedAmount(claim.getClaimedAmount())
            .approvedAmount(claim.getApprovedAmount())
            .settledAmount(claim.getSettledAmount())
            .comment(comment)
            .rejectionReason(claim.getRejectionReason())
            .changedByUserId(userId)
            .changedByRole(role)
            .eventTimestamp(LocalDateTime.now())
            .build();
    }

    /**
     * Generate a unique claim number: CLM-YYYY-NNNNNN
     * Uses random suffix to avoid enumeration attacks.
     */
    private String generateClaimNumber() {
        String year = String.valueOf(LocalDate.now().getYear());
        String suffix;
        String claimNumber;
        do {
            suffix = String.format("%06d", new Random().nextInt(999999));
            claimNumber = "CLM-" + year + "-" + suffix;
        } while (claimRepository.existsByClaimNumber(claimNumber));
        return claimNumber;
    }

    /** Auto-assign priority based on claim amount */
    private Claim.Priority calculatePriority(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.valueOf(500000)) >= 0) return Claim.Priority.URGENT;
        if (amount.compareTo(BigDecimal.valueOf(100000)) >= 0) return Claim.Priority.HIGH;
        if (amount.compareTo(BigDecimal.valueOf(10000))  >= 0) return Claim.Priority.NORMAL;
        return Claim.Priority.LOW;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new InvalidClaimOperationException("File is empty");
        String ct = file.getContentType();
        if (ct == null || (!ct.startsWith("image/") && !ct.equals("application/pdf")))
            throw new InvalidClaimOperationException("Only image and PDF files are allowed");
        if (file.getSize() > 15 * 1024 * 1024)
            throw new InvalidClaimOperationException("File must not exceed 15MB");
    }

    private String storeFile(MultipartFile file, Long claimId) {
        try {
            String ext = Optional.ofNullable(file.getOriginalFilename())
                .filter(f -> f.contains(".")).map(f -> f.substring(f.lastIndexOf("."))).orElse("");
            String filename = claimId + "_" + UUID.randomUUID() + ext;
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            return baseUrl + "/" + filename;
        } catch (IOException e) {
            throw new InvalidClaimOperationException("Could not store file: " + e.getMessage());
        }
    }
}
