package com.insurance.policy.repository;

import com.insurance.policy.entity.Policy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    Optional<Policy> findByPolicyNumber(String policyNumber);

    List<Policy> findByPolicyHolderIdOrderByCreatedAtDesc(Long policyHolderId);

    Page<Policy> findByPolicyHolderId(Long policyHolderId, Pageable pageable);

    Page<Policy> findByStatus(Policy.PolicyStatus status, Pageable pageable);

    boolean existsByPolicyNumber(String policyNumber);

    // For claim-service to validate coverage
    @Query("SELECT p FROM Policy p WHERE p.policyNumber = :policyNumber AND p.policyHolderId = :holderId AND p.status = 'ACTIVE'")
    Optional<Policy> findActivePolicyForHolder(@Param("policyNumber") String policyNumber,
                                                @Param("holderId") Long holderId);

    // Update claimed amount when a claim is settled
    @Modifying
    @Transactional
    @Query("UPDATE Policy p SET p.claimedAmount = p.claimedAmount + :amount WHERE p.policyNumber = :policyNumber")
    void incrementClaimedAmount(@Param("policyNumber") String policyNumber,
                                @Param("amount") BigDecimal amount);
}
