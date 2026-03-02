package com.insurance.policy.mapper;

import com.insurance.policy.dto.request.CreatePolicyRequest;
import com.insurance.policy.dto.response.PolicyResponse;
import com.insurance.policy.entity.Policy;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PolicyMapper {

    @Mapping(target = "remainingCoverage", expression = "java(policy.getRemainingCoverage())")
    @Mapping(target = "expired",           expression = "java(policy.isExpired())")
    PolicyResponse toResponse(Policy policy);

    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "policyNumber",  ignore = true)
    @Mapping(target = "status",        ignore = true)
    @Mapping(target = "claimedAmount", ignore = true)
    @Mapping(target = "createdAt",     ignore = true)
    @Mapping(target = "updatedAt",     ignore = true)
    Policy fromCreateRequest(CreatePolicyRequest request);
}
