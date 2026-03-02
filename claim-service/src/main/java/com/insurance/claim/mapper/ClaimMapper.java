package com.insurance.claim.mapper;

import com.insurance.claim.dto.response.*;
import com.insurance.claim.entity.*;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClaimMapper {

    @Mapping(target = "statusLabel",   expression = "java(formatStatus(claim.getStatus()))")
    @Mapping(target = "documentCount", expression = "java(claim.getDocuments().size())")
    ClaimSummaryResponse toSummary(Claim claim);

    @Mapping(target = "statusLabel", expression = "java(formatStatus(claim.getStatus()))")
    ClaimResponse toResponse(Claim claim);

    ClaimDocumentResponse toDocumentResponse(ClaimDocument document);

    ClaimNoteResponse toNoteResponse(ClaimNote note);

    ClaimStatusHistoryResponse toHistoryResponse(ClaimStatusHistory history);

    List<ClaimDocumentResponse> toDocumentResponseList(List<ClaimDocument> docs);
    List<ClaimNoteResponse>     toNoteResponseList(List<ClaimNote> notes);
    List<ClaimStatusHistoryResponse> toHistoryResponseList(List<ClaimStatusHistory> history);

    default String formatStatus(Claim.ClaimStatus status) {
        if (status == null) return "";
        return status.name().replace("_", " ");
    }
}
