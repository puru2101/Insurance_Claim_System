package com.insurance.user.mapper;

import com.insurance.user.dto.request.CreateProfileRequest;
import com.insurance.user.dto.request.UpdateProfileRequest;
import com.insurance.user.dto.response.UserDocumentResponse;
import com.insurance.user.dto.response.UserProfileResponse;
import com.insurance.user.entity.UserDocument;
import com.insurance.user.entity.UserProfile;
import org.mapstruct.*;

/**
 * MapStruct Mapper — compile-time generated, zero reflection.
 *
 * @BeanMapping(nullValuePropertyMappingStrategy = IGNORE_BY_SOURCE_VALUE)
 * Means: when updating, only set fields that are non-null in the request.
 * This enables partial updates (PATCH semantics) without overwriting existing data.
 */
@Mapper(componentModel = "spring")
public interface UserProfileMapper {

    @Mapping(target = "fullName",
        expression = "java(profile.getFirstName() + ' ' + profile.getLastName())")
    UserProfileResponse toResponse(UserProfile profile);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "policyNumber", ignore = true)
    @Mapping(target = "nationalId", ignore = true)
    @Mapping(target = "profilePictureUrl", ignore = true)
    @Mapping(target = "accountStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserProfile fromCreateRequest(CreateProfileRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authUserId", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "policyNumber", ignore = true)
    @Mapping(target = "nationalId", ignore = true)
    @Mapping(target = "profilePictureUrl", ignore = true)
    @Mapping(target = "accountStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromRequest(UpdateProfileRequest request, @MappingTarget UserProfile profile);

    UserDocumentResponse toDocumentResponse(UserDocument document);
}
