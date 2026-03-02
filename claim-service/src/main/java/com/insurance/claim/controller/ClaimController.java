package com.insurance.claim.controller;

import com.insurance.claim.dto.request.*;
import com.insurance.claim.dto.response.*;
import com.insurance.claim.entity.Claim;
import com.insurance.claim.service.ClaimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Collection;

/**
 * ClaimController
 *
 * Base path: /claims  (gateway: /api/claims/**)
 *
 * Customer endpoints:
 *   POST   /claims                       → submit new claim
 *   GET    /claims/my                    → list own claims (paginated + filter by status)
 *   GET    /claims/{claimNumber}         → get claim details
 *   PATCH  /claims/{claimId}/withdraw    → withdraw claim
 *   POST   /claims/{claimId}/notes       → add note to claim
 *   POST   /claims/{claimId}/documents   → upload supporting document
 *   DELETE /claims/{claimId}/documents/{docId}
 *
 * Agent / Admin endpoints:
 *   GET    /claims/search                → search all claims (filters)
 *   PATCH  /claims/{claimId}/status      → update claim status (approve/reject/settle)
 *   PATCH  /claims/{claimId}/assign      → assign agent to claim
 *   GET    /claims/stats                 → dashboard statistics
 */
@Slf4j
@RestController
@RequestMapping("/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    // ── Customer: Submit claim ─────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<ClaimResponse>> submitClaim(
            @Valid @RequestBody SubmitClaimRequest request,
            Authentication auth) {

        Long customerId = getAuthUserId(auth);
        ClaimResponse claim = claimService.submitClaim(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Claim submitted successfully. Claim number: " + claim.getClaimNumber(), claim));
    }

    // ── Customer: List own claims ──────────────────────────────────────────────

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PagedResponse<ClaimSummaryResponse>>> getMyClaimsResponse(
            @RequestParam(required = false) Claim.ClaimStatus status,
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "10")   int size,
            Authentication auth) {

        Long customerId = getAuthUserId(auth);
        Pageable pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());
        PagedResponse<ClaimSummaryResponse> claims = claimService.getCustomerClaims(customerId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Claims retrieved", claims));
    }

    // ── Get claim by number (customer sees own; agent sees all) ───────────────

    @GetMapping("/{claimNumber}")
    public ResponseEntity<ApiResponse<ClaimResponse>> getClaim(
            @PathVariable String claimNumber,
            Authentication auth) {

        Long userId  = getAuthUserId(auth);
        boolean isAgent = hasRole(auth, "ROLE_AGENT") || hasRole(auth, "ROLE_ADMIN");
        ClaimResponse claim = claimService.getClaimByNumber(claimNumber, userId, isAgent);
        return ResponseEntity.ok(ApiResponse.success("Claim retrieved", claim));
    }

    // ── Customer: Withdraw ─────────────────────────────────────────────────────

    @PatchMapping("/{claimId}/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdrawClaim(
            @PathVariable Long claimId,
            Authentication auth) {

        claimService.withdrawClaim(claimId, getAuthUserId(auth));
        return ResponseEntity.ok(ApiResponse.success("Claim withdrawn successfully"));
    }

    // ── Agent/Admin: Update status ─────────────────────────────────────────────

    @PatchMapping("/{claimId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<ApiResponse<ClaimResponse>> updateClaimStatus(
            @PathVariable Long claimId,
            @Valid @RequestBody UpdateClaimStatusRequest request,
            Authentication auth) {

        Long agentId   = getAuthUserId(auth);
        String agentRole = getPrimaryRole(auth);
        ClaimResponse claim = claimService.updateClaimStatus(claimId, request, agentId, agentRole);
        return ResponseEntity.ok(ApiResponse.success("Claim status updated to: " + request.getNewStatus(), claim));
    }

    // ── Admin: Assign agent ────────────────────────────────────────────────────

    @PatchMapping("/{claimId}/assign")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<ClaimResponse>> assignAgent(
            @PathVariable Long claimId,
            @Valid @RequestBody AssignAgentRequest request) {

        ClaimResponse claim = claimService.assignAgent(claimId, request.getAgentId());
        return ResponseEntity.ok(ApiResponse.success("Agent assigned successfully", claim));
    }

    // ── Agent/Admin: Search ────────────────────────────────────────────────────

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<ClaimSummaryResponse>>> searchClaims(
            @RequestParam(required = false) Claim.ClaimStatus status,
            @RequestParam(required = false) Claim.ClaimType  claimType,
            @RequestParam(required = false) Long             customerId,
            @RequestParam(required = false) Long             agentId,
            @RequestParam(required = false) String           policyNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size,
            @RequestParam(defaultValue = "submittedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC")
            ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PagedResponse<ClaimSummaryResponse> claims = claimService.searchClaims(
            status, claimType, customerId, agentId, policyNumber, fromDate, toDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Search results", claims));
    }

    // ── Notes ──────────────────────────────────────────────────────────────────

    @PostMapping("/{claimId}/notes")
    public ResponseEntity<ApiResponse<ClaimNoteResponse>> addNote(
            @PathVariable Long claimId,
            @Valid @RequestBody AddNoteRequest request,
            Authentication auth) {

        Long userId    = getAuthUserId(auth);
        String role    = getPrimaryRole(auth);
        // Customers cannot add internal notes
        if (!hasRole(auth, "ROLE_AGENT") && !hasRole(auth, "ROLE_ADMIN")) {
            request.setInternal(false);
        }
        ClaimNoteResponse note = claimService.addNote(claimId, request, userId, role);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Note added", note));
    }

    // ── Documents ──────────────────────────────────────────────────────────────

    @PostMapping(value = "/{claimId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ClaimDocumentResponse>> uploadDocument(
            @PathVariable Long claimId,
            @RequestParam("file")         MultipartFile file,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "description", required = false) String description,
            Authentication auth) {

        Long userId = getAuthUserId(auth);
        ClaimDocumentResponse doc = claimService.uploadDocument(claimId, file, documentType, description, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Document uploaded", doc));
    }

    @DeleteMapping("/{claimId}/documents/{documentId}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable Long claimId,
            @PathVariable Long documentId,
            Authentication auth) {

        claimService.deleteDocument(claimId, documentId, getAuthUserId(auth));
        return ResponseEntity.ok(ApiResponse.success("Document deleted"));
    }

    // ── Stats ──────────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<ApiResponse<ClaimStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success("Statistics retrieved", claimService.getClaimStats()));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Long getAuthUserId(Authentication auth) {
        Object details = auth.getDetails();
        if (details instanceof String s && !s.isEmpty()) return Long.parseLong(s);
        throw new RuntimeException("Unable to extract user ID from authentication context");
    }

    private boolean hasRole(Authentication auth, String role) {
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        return authorities.stream().anyMatch(a -> a.getAuthority().equals(role));
    }

    private String getPrimaryRole(Authentication auth) {
        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(r -> r.startsWith("ROLE_"))
            .findFirst().orElse("ROLE_CUSTOMER");
    }
}
