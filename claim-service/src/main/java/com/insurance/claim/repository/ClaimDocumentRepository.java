package com.insurance.claim.repository;

import com.insurance.claim.entity.ClaimDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimDocumentRepository extends JpaRepository<ClaimDocument, Long> {
    List<ClaimDocument> findByClaimIdOrderByUploadedAtDesc(Long claimId);
    Optional<ClaimDocument> findByIdAndClaimId(Long id, Long claimId);
    long countByClaimId(Long claimId);
}
