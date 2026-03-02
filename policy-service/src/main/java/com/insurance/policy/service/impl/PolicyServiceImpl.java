package com.insurance.policy.service.impl;

import com.insurance.policy.dto.request.CreatePolicyRequest;
import com.insurance.policy.dto.response.PolicyResponse;
import com.insurance.policy.entity.Policy;
import com.insurance.policy.exception.DuplicateResourceException;
import com.insurance.policy.exception.ResourceNotFoundException;
import com.insurance.policy.mapper.PolicyMapper;
import com.insurance.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyServiceImpl {

    private final PolicyRepository policyRepository;
    private final PolicyMapper mapper;

    @Transactional
    public PolicyResponse createPolicy(CreatePolicyRequest request) {
        Policy policy = mapper.fromCreateRequest(request);
        policy.setPolicyNumber(generatePolicyNumber(request.getPolicyType()));
        policy.setStatus(Policy.PolicyStatus.ACTIVE);
        policy.setClaimedAmount(BigDecimal.ZERO);
        policy.setNextPremiumDueDate(calculateNextPremiumDate(request.getStartDate(), request.getPremiumFrequency()));

        Policy saved = policyRepository.save(policy);
        log.info("Policy created: {} for holder: {}", saved.getPolicyNumber(), saved.getPolicyHolderId());
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PolicyResponse getPolicyByNumber(String policyNumber) {
        return mapper.toResponse(policyRepository.findByPolicyNumber(policyNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyNumber)));
    }

    @Transactional(readOnly = true)
    public List<PolicyResponse> getPoliciesByHolder(Long holderId) {
        return policyRepository.findByPolicyHolderIdOrderByCreatedAtDesc(holderId)
            .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<PolicyResponse> getAllPolicies(Pageable pageable) {
        return policyRepository.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional
    public PolicyResponse cancelPolicy(String policyNumber, Long requestingUserId) {
        Policy policy = policyRepository.findByPolicyNumber(policyNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyNumber));
        policy.setStatus(Policy.PolicyStatus.CANCELLED);
        log.info("Policy cancelled: {}", policyNumber);
        return mapper.toResponse(policyRepository.save(policy));
    }

    /** Called by Kafka consumer when a claim is settled — updates utilization */
    @Transactional
    public void updateClaimedAmount(String policyNumber, BigDecimal settledAmount) {
        policyRepository.incrementClaimedAmount(policyNumber, settledAmount);
        log.info("Updated claimed amount for policy {}: +{}", policyNumber, settledAmount);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String generatePolicyNumber(Policy.PolicyType type) {
        String prefix = switch (type) {
            case HEALTH             -> "HLT";
            case MOTOR              -> "MTR";
            case LIFE               -> "LIF";
            case PROPERTY           -> "PRP";
            case TRAVEL             -> "TRV";
            case PERSONAL_ACCIDENT  -> "PAC";
            case LIABILITY          -> "LBL";
            default                 -> "POL";
        };
        String year   = String.valueOf(LocalDate.now().getYear());
        String suffix = String.format("%06d", new Random().nextInt(999999));
        String number = prefix + "-" + year + "-" + suffix;
        // Ensure uniqueness
        while (policyRepository.existsByPolicyNumber(number)) {
            suffix = String.format("%06d", new Random().nextInt(999999));
            number = prefix + "-" + year + "-" + suffix;
        }
        return number;
    }

    private LocalDate calculateNextPremiumDate(LocalDate startDate, Policy.PremiumFrequency freq) {
        return switch (freq) {
            case MONTHLY     -> startDate.plusMonths(1);
            case QUARTERLY   -> startDate.plusMonths(3);
            case SEMI_ANNUAL -> startDate.plusMonths(6);
            case ANNUAL      -> startDate.plusYears(1);
        };
    }
}
