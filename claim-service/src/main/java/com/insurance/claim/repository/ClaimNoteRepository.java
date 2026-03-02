package com.insurance.claim.repository;

import com.insurance.claim.entity.ClaimNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClaimNoteRepository extends JpaRepository<ClaimNote, Long> {

    // Customers see only non-internal notes; agents see all
    @Query("SELECT n FROM ClaimNote n WHERE n.claim.id = :claimId " +
           "AND (:includeInternal = true OR n.isInternal = false) " +
           "ORDER BY n.createdAt DESC")
    List<ClaimNote> findByClaimId(@Param("claimId") Long claimId,
                                  @Param("includeInternal") boolean includeInternal);
}
