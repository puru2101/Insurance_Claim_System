package com.insurance.policy.controller;

import com.insurance.policy.dto.request.CreatePolicyRequest;
import com.insurance.policy.dto.response.ApiResponse;
import com.insurance.policy.dto.response.PolicyResponse;
import com.insurance.policy.service.impl.PolicyServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PolicyController
 * Base path: /policies  (gateway: /api/policies/**)
 *
 *   POST   /policies                   → create policy (AGENT/ADMIN)
 *   GET    /policies/my                → customer's own policies
 *   GET    /policies/{policyNumber}    → get by number
 *   DELETE /policies/{policyNumber}    → cancel policy (ADMIN)
 *   GET    /policies/admin             → all policies paginated (AGENT/ADMIN)
 */
@RestController
@RequestMapping("/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyServiceImpl policyService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<ApiResponse<PolicyResponse>> createPolicy(
            @Valid @RequestBody CreatePolicyRequest request) {
        PolicyResponse policy = policyService.createPolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Policy created: " + policy.getPolicyNumber(), policy));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<PolicyResponse>>> getMyPolicies(Authentication auth) {
        Long userId = getAuthUserId(auth);
        List<PolicyResponse> policies = policyService.getPoliciesByHolder(userId);
        return ResponseEntity.ok(ApiResponse.success("Policies retrieved", policies));
    }

    @GetMapping("/{policyNumber}")
    public ResponseEntity<ApiResponse<PolicyResponse>> getPolicy(@PathVariable String policyNumber) {
        return ResponseEntity.ok(ApiResponse.success("Policy retrieved",
            policyService.getPolicyByNumber(policyNumber)));
    }

    @DeleteMapping("/{policyNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PolicyResponse>> cancelPolicy(
            @PathVariable String policyNumber, Authentication auth) {
        PolicyResponse policy = policyService.cancelPolicy(policyNumber, getAuthUserId(auth));
        return ResponseEntity.ok(ApiResponse.success("Policy cancelled", policy));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<ApiResponse<Page<PolicyResponse>>> getAllPolicies(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PolicyResponse> policies = policyService.getAllPolicies(
            PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success("Policies retrieved", policies));
    }

    private Long getAuthUserId(Authentication auth) {
        Object details = auth.getDetails();
        if (details instanceof String s && !s.isEmpty()) return Long.parseLong(s);
        throw new RuntimeException("Cannot extract user ID from auth context");
    }
}
