package com.insurance.claim.repository;

import com.insurance.claim.entity.Claim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    Optional<Claim> findByClaimNumber(String claimNumber);

    Optional<Claim> findByIdAndCustomerId(Long id, Long customerId);

    boolean existsByClaimNumber(String claimNumber);

    // ── Customer queries ──────────────────────────────────────────────────────

    Page<Claim> findByCustomerIdOrderBySubmittedAtDesc(Long customerId, Pageable pageable);

    Page<Claim> findByCustomerIdAndStatusOrderBySubmittedAtDesc(
        Long customerId, Claim.ClaimStatus status, Pageable pageable);

    // ── Agent queries ─────────────────────────────────────────────────────────

    Page<Claim> findByAssignedAgentIdOrderByPriorityAscSubmittedAtAsc(
        Long agentId, Pageable pageable);

    // ── Admin: flexible search ─────────────────────────────────────────────────

    @Query("""
        SELECT c FROM Claim c WHERE
          (:status       IS NULL OR c.status = :status) AND
          (:claimType    IS NULL OR c.claimType = :claimType) AND
          (:customerId   IS NULL OR c.customerId = :customerId) AND
          (:agentId      IS NULL OR c.assignedAgentId = :agentId) AND
          (:policyNumber IS NULL OR c.policyNumber = :policyNumber) AND
          (:fromDate     IS NULL OR c.incidentDate >= :fromDate) AND
          (:toDate       IS NULL OR c.incidentDate <= :toDate)
        """)
    Page<Claim> searchClaims(
        @Param("status")       Claim.ClaimStatus status,
        @Param("claimType")    Claim.ClaimType claimType,
        @Param("customerId")   Long customerId,
        @Param("agentId")      Long agentId,
        @Param("policyNumber") String policyNumber,
        @Param("fromDate")     LocalDate fromDate,
        @Param("toDate")       LocalDate toDate,
        Pageable pageable);

    // ── Statistics queries ─────────────────────────────────────────────────────

    long countByStatus(Claim.ClaimStatus status);

    @Query("SELECT COALESCE(SUM(c.claimedAmount), 0) FROM Claim c")
    BigDecimal sumTotalClaimedAmount();

    @Query("SELECT COALESCE(SUM(c.approvedAmount), 0) FROM Claim c WHERE c.approvedAmount IS NOT NULL")
    BigDecimal sumTotalApprovedAmount();

    @Query("SELECT COALESCE(SUM(c.settledAmount), 0) FROM Claim c WHERE c.settledAmount IS NOT NULL")
    BigDecimal sumTotalSettledAmount();

    @Query("SELECT c.claimType, COUNT(c) FROM Claim c GROUP BY c.claimType")
    java.util.List<Object[]> countByClaimType();

    @Query("""
        SELECT AVG(DATEDIFF(c.resolvedAt, c.submittedAt))
        FROM Claim c
        WHERE c.resolvedAt IS NOT NULL
        """)
    Double avgResolutionDays();

    // ── Agent workload ─────────────────────────────────────────────────────────

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE Claim c SET c.assignedAgentId = :agentId WHERE c.id = :claimId")
    void assignAgent(@Param("claimId") Long claimId, @Param("agentId") Long agentId);
}
