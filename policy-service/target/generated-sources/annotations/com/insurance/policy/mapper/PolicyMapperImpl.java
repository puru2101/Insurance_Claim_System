package com.insurance.policy.mapper;

import com.insurance.policy.dto.request.CreatePolicyRequest;
import com.insurance.policy.dto.response.PolicyResponse;
import com.insurance.policy.entity.Policy;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-28T12:12:37+0530",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.37.0.v20240215-1558, environment: Java 17.0.11 (Eclipse Adoptium)"
)
@Component
public class PolicyMapperImpl implements PolicyMapper {

    @Override
    public PolicyResponse toResponse(Policy policy) {
        if ( policy == null ) {
            return null;
        }

        PolicyResponse.PolicyResponseBuilder policyResponse = PolicyResponse.builder();

        policyResponse.claimedAmount( policy.getClaimedAmount() );
        policyResponse.coverageDetails( policy.getCoverageDetails() );
        policyResponse.createdAt( policy.getCreatedAt() );
        policyResponse.deductible( policy.getDeductible() );
        policyResponse.exclusions( policy.getExclusions() );
        policyResponse.expiryDate( policy.getExpiryDate() );
        policyResponse.id( policy.getId() );
        policyResponse.nextPremiumDueDate( policy.getNextPremiumDueDate() );
        policyResponse.nomineeName( policy.getNomineeName() );
        policyResponse.nomineeRelationship( policy.getNomineeRelationship() );
        policyResponse.policyHolderId( policy.getPolicyHolderId() );
        policyResponse.policyNumber( policy.getPolicyNumber() );
        policyResponse.policyType( policy.getPolicyType() );
        policyResponse.premiumAmount( policy.getPremiumAmount() );
        policyResponse.premiumFrequency( policy.getPremiumFrequency() );
        policyResponse.startDate( policy.getStartDate() );
        policyResponse.status( policy.getStatus() );
        policyResponse.sumInsured( policy.getSumInsured() );
        policyResponse.updatedAt( policy.getUpdatedAt() );

        policyResponse.remainingCoverage( policy.getRemainingCoverage() );
        policyResponse.expired( policy.isExpired() );

        return policyResponse.build();
    }

    @Override
    public Policy fromCreateRequest(CreatePolicyRequest request) {
        if ( request == null ) {
            return null;
        }

        Policy.PolicyBuilder policy = Policy.builder();

        policy.coverageDetails( request.getCoverageDetails() );
        policy.deductible( request.getDeductible() );
        policy.exclusions( request.getExclusions() );
        policy.expiryDate( request.getExpiryDate() );
        policy.nomineeName( request.getNomineeName() );
        policy.nomineeRelationship( request.getNomineeRelationship() );
        policy.policyHolderId( request.getPolicyHolderId() );
        policy.policyType( request.getPolicyType() );
        policy.premiumAmount( request.getPremiumAmount() );
        policy.premiumFrequency( request.getPremiumFrequency() );
        policy.startDate( request.getStartDate() );
        policy.sumInsured( request.getSumInsured() );

        return policy.build();
    }
}
