package com.insurance.user.repository;

import com.insurance.user.entity.UserDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDocumentRepository extends JpaRepository<UserDocument, Long> {

    List<UserDocument> findByAuthUserIdOrderByUploadedAtDesc(Long authUserId);

    Optional<UserDocument> findByIdAndAuthUserId(Long id, Long authUserId);

    boolean existsByAuthUserIdAndDocumentType(Long authUserId, UserDocument.DocumentType type);
}
