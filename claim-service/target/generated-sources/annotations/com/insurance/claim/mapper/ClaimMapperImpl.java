package com.insurance.claim.mapper;

import com.insurance.claim.dto.response.ClaimDocumentResponse;
import com.insurance.claim.dto.response.ClaimNoteResponse;
import com.insurance.claim.dto.response.ClaimResponse;
import com.insurance.claim.dto.response.ClaimStatusHistoryResponse;
import com.insurance.claim.dto.response.ClaimSummaryResponse;
import com.insurance.claim.entity.Claim;
import com.insurance.claim.entity.ClaimDocument;
import com.insurance.claim.entity.ClaimNote;
import com.insurance.claim.entity.ClaimStatusHistory;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-02T08:21:39+0530",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.37.0.v20240215-1558, environment: Java 17.0.11 (Eclipse Adoptium)"
)
@Component
public class ClaimMapperImpl implements ClaimMapper {

    @Override
    public ClaimSummaryResponse toSummary(Claim claim) {
        if ( claim == null ) {
            return null;
        }

        ClaimSummaryResponse.ClaimSummaryResponseBuilder claimSummaryResponse = ClaimSummaryResponse.builder();

        claimSummaryResponse.approvedAmount( claim.getApprovedAmount() );
        claimSummaryResponse.assignedAgentId( claim.getAssignedAgentId() );
        claimSummaryResponse.claimNumber( claim.getClaimNumber() );
        claimSummaryResponse.claimType( claim.getClaimType() );
        claimSummaryResponse.claimedAmount( claim.getClaimedAmount() );
        claimSummaryResponse.id( claim.getId() );
        claimSummaryResponse.incidentDate( claim.getIncidentDate() );
        claimSummaryResponse.priority( claim.getPriority() );
        claimSummaryResponse.status( claim.getStatus() );
        claimSummaryResponse.submittedAt( claim.getSubmittedAt() );
        claimSummaryResponse.updatedAt( claim.getUpdatedAt() );

        claimSummaryResponse.statusLabel( formatStatus(claim.getStatus()) );
        claimSummaryResponse.documentCount( claim.getDocuments().size() );

        return claimSummaryResponse.build();
    }

    @Override
    public ClaimResponse toResponse(Claim claim) {
        if ( claim == null ) {
            return null;
        }

        ClaimResponse.ClaimResponseBuilder claimResponse = ClaimResponse.builder();

        claimResponse.approvedAmount( claim.getApprovedAmount() );
        claimResponse.assignedAgentId( claim.getAssignedAgentId() );
        claimResponse.claimNumber( claim.getClaimNumber() );
        claimResponse.claimType( claim.getClaimType() );
        claimResponse.claimedAmount( claim.getClaimedAmount() );
        claimResponse.coverageLimit( claim.getCoverageLimit() );
        claimResponse.customerId( claim.getCustomerId() );
        claimResponse.documents( toDocumentResponseList( claim.getDocuments() ) );
        claimResponse.id( claim.getId() );
        claimResponse.incidentDate( claim.getIncidentDate() );
        claimResponse.incidentDescription( claim.getIncidentDescription() );
        claimResponse.incidentLocation( claim.getIncidentLocation() );
        claimResponse.notes( toNoteResponseList( claim.getNotes() ) );
        claimResponse.policyNumber( claim.getPolicyNumber() );
        claimResponse.priority( claim.getPriority() );
        claimResponse.rejectionReason( claim.getRejectionReason() );
        claimResponse.resolvedAt( claim.getResolvedAt() );
        claimResponse.reviewStartedAt( claim.getReviewStartedAt() );
        claimResponse.settledAmount( claim.getSettledAmount() );
        claimResponse.settledAt( claim.getSettledAt() );
        claimResponse.settlementNotes( claim.getSettlementNotes() );
        claimResponse.status( claim.getStatus() );
        claimResponse.statusHistory( toHistoryResponseList( claim.getStatusHistory() ) );
        claimResponse.submittedAt( claim.getSubmittedAt() );
        claimResponse.updatedAt( claim.getUpdatedAt() );

        claimResponse.statusLabel( formatStatus(claim.getStatus()) );

        return claimResponse.build();
    }

    @Override
    public ClaimDocumentResponse toDocumentResponse(ClaimDocument document) {
        if ( document == null ) {
            return null;
        }

        ClaimDocumentResponse.ClaimDocumentResponseBuilder claimDocumentResponse = ClaimDocumentResponse.builder();

        claimDocumentResponse.description( document.getDescription() );
        claimDocumentResponse.documentType( document.getDocumentType() );
        claimDocumentResponse.fileName( document.getFileName() );
        claimDocumentResponse.fileSizeBytes( document.getFileSizeBytes() );
        claimDocumentResponse.fileUrl( document.getFileUrl() );
        claimDocumentResponse.id( document.getId() );
        claimDocumentResponse.mimeType( document.getMimeType() );
        claimDocumentResponse.uploadedAt( document.getUploadedAt() );
        claimDocumentResponse.uploadedByUserId( document.getUploadedByUserId() );

        return claimDocumentResponse.build();
    }

    @Override
    public ClaimNoteResponse toNoteResponse(ClaimNote note) {
        if ( note == null ) {
            return null;
        }

        ClaimNoteResponse.ClaimNoteResponseBuilder claimNoteResponse = ClaimNoteResponse.builder();

        claimNoteResponse.authorRole( note.getAuthorRole() );
        claimNoteResponse.authorUserId( note.getAuthorUserId() );
        claimNoteResponse.content( note.getContent() );
        claimNoteResponse.createdAt( note.getCreatedAt() );
        claimNoteResponse.id( note.getId() );
        claimNoteResponse.isInternal( note.getIsInternal() );

        return claimNoteResponse.build();
    }

    @Override
    public ClaimStatusHistoryResponse toHistoryResponse(ClaimStatusHistory history) {
        if ( history == null ) {
            return null;
        }

        ClaimStatusHistoryResponse.ClaimStatusHistoryResponseBuilder claimStatusHistoryResponse = ClaimStatusHistoryResponse.builder();

        claimStatusHistoryResponse.changedAt( history.getChangedAt() );
        claimStatusHistoryResponse.changedByRole( history.getChangedByRole() );
        claimStatusHistoryResponse.changedByUserId( history.getChangedByUserId() );
        claimStatusHistoryResponse.comment( history.getComment() );
        claimStatusHistoryResponse.fromStatus( history.getFromStatus() );
        claimStatusHistoryResponse.id( history.getId() );
        claimStatusHistoryResponse.toStatus( history.getToStatus() );

        return claimStatusHistoryResponse.build();
    }

    @Override
    public List<ClaimDocumentResponse> toDocumentResponseList(List<ClaimDocument> docs) {
        if ( docs == null ) {
            return null;
        }

        List<ClaimDocumentResponse> list = new ArrayList<ClaimDocumentResponse>( docs.size() );
        for ( ClaimDocument claimDocument : docs ) {
            list.add( toDocumentResponse( claimDocument ) );
        }

        return list;
    }

    @Override
    public List<ClaimNoteResponse> toNoteResponseList(List<ClaimNote> notes) {
        if ( notes == null ) {
            return null;
        }

        List<ClaimNoteResponse> list = new ArrayList<ClaimNoteResponse>( notes.size() );
        for ( ClaimNote claimNote : notes ) {
            list.add( toNoteResponse( claimNote ) );
        }

        return list;
    }

    @Override
    public List<ClaimStatusHistoryResponse> toHistoryResponseList(List<ClaimStatusHistory> history) {
        if ( history == null ) {
            return null;
        }

        List<ClaimStatusHistoryResponse> list = new ArrayList<ClaimStatusHistoryResponse>( history.size() );
        for ( ClaimStatusHistory claimStatusHistory : history ) {
            list.add( toHistoryResponse( claimStatusHistory ) );
        }

        return list;
    }
}
