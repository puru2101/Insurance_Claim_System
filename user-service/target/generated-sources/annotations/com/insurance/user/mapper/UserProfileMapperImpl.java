package com.insurance.user.mapper;

import com.insurance.user.dto.request.CreateProfileRequest;
import com.insurance.user.dto.request.UpdateProfileRequest;
import com.insurance.user.dto.response.UserDocumentResponse;
import com.insurance.user.dto.response.UserProfileResponse;
import com.insurance.user.entity.UserDocument;
import com.insurance.user.entity.UserProfile;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-28T12:12:40+0530",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.37.0.v20240215-1558, environment: Java 17.0.11 (Eclipse Adoptium)"
)
@Component
public class UserProfileMapperImpl implements UserProfileMapper {

    @Override
    public UserProfileResponse toResponse(UserProfile profile) {
        if ( profile == null ) {
            return null;
        }

        UserProfileResponse.UserProfileResponseBuilder userProfileResponse = UserProfileResponse.builder();

        userProfileResponse.accountStatus( profile.getAccountStatus() );
        userProfileResponse.addressLine1( profile.getAddressLine1() );
        userProfileResponse.addressLine2( profile.getAddressLine2() );
        userProfileResponse.annualIncome( profile.getAnnualIncome() );
        userProfileResponse.authUserId( profile.getAuthUserId() );
        userProfileResponse.city( profile.getCity() );
        userProfileResponse.country( profile.getCountry() );
        userProfileResponse.createdAt( profile.getCreatedAt() );
        userProfileResponse.dateOfBirth( profile.getDateOfBirth() );
        userProfileResponse.email( profile.getEmail() );
        userProfileResponse.firstName( profile.getFirstName() );
        userProfileResponse.gender( profile.getGender() );
        userProfileResponse.id( profile.getId() );
        userProfileResponse.lastName( profile.getLastName() );
        userProfileResponse.notificationEmail( profile.getNotificationEmail() );
        userProfileResponse.notificationSms( profile.getNotificationSms() );
        userProfileResponse.occupation( profile.getOccupation() );
        userProfileResponse.phoneNumber( profile.getPhoneNumber() );
        userProfileResponse.policyNumber( profile.getPolicyNumber() );
        userProfileResponse.postalCode( profile.getPostalCode() );
        userProfileResponse.preferredLanguage( profile.getPreferredLanguage() );
        userProfileResponse.profilePictureUrl( profile.getProfilePictureUrl() );
        userProfileResponse.state( profile.getState() );
        userProfileResponse.updatedAt( profile.getUpdatedAt() );

        userProfileResponse.fullName( profile.getFirstName() + ' ' + profile.getLastName() );

        return userProfileResponse.build();
    }

    @Override
    public UserProfile fromCreateRequest(CreateProfileRequest request) {
        if ( request == null ) {
            return null;
        }

        UserProfile.UserProfileBuilder userProfile = UserProfile.builder();

        userProfile.authUserId( request.getAuthUserId() );
        userProfile.email( request.getEmail() );
        userProfile.firstName( request.getFirstName() );
        userProfile.lastName( request.getLastName() );
        userProfile.phoneNumber( request.getPhoneNumber() );

        return userProfile.build();
    }

    @Override
    public void updateFromRequest(UpdateProfileRequest request, UserProfile profile) {
        if ( request == null ) {
            return;
        }

        if ( request.getAddressLine1() != null ) {
            profile.setAddressLine1( request.getAddressLine1() );
        }
        if ( request.getAddressLine2() != null ) {
            profile.setAddressLine2( request.getAddressLine2() );
        }
        if ( request.getAnnualIncome() != null ) {
            profile.setAnnualIncome( request.getAnnualIncome() );
        }
        if ( request.getCity() != null ) {
            profile.setCity( request.getCity() );
        }
        if ( request.getCountry() != null ) {
            profile.setCountry( request.getCountry() );
        }
        if ( request.getDateOfBirth() != null ) {
            profile.setDateOfBirth( request.getDateOfBirth() );
        }
        if ( request.getFirstName() != null ) {
            profile.setFirstName( request.getFirstName() );
        }
        if ( request.getGender() != null ) {
            profile.setGender( request.getGender() );
        }
        if ( request.getLastName() != null ) {
            profile.setLastName( request.getLastName() );
        }
        if ( request.getNotificationEmail() != null ) {
            profile.setNotificationEmail( request.getNotificationEmail() );
        }
        if ( request.getNotificationSms() != null ) {
            profile.setNotificationSms( request.getNotificationSms() );
        }
        if ( request.getOccupation() != null ) {
            profile.setOccupation( request.getOccupation() );
        }
        if ( request.getPhoneNumber() != null ) {
            profile.setPhoneNumber( request.getPhoneNumber() );
        }
        if ( request.getPostalCode() != null ) {
            profile.setPostalCode( request.getPostalCode() );
        }
        if ( request.getPreferredLanguage() != null ) {
            profile.setPreferredLanguage( request.getPreferredLanguage() );
        }
        if ( request.getState() != null ) {
            profile.setState( request.getState() );
        }
    }

    @Override
    public UserDocumentResponse toDocumentResponse(UserDocument document) {
        if ( document == null ) {
            return null;
        }

        UserDocumentResponse.UserDocumentResponseBuilder userDocumentResponse = UserDocumentResponse.builder();

        userDocumentResponse.authUserId( document.getAuthUserId() );
        userDocumentResponse.documentType( document.getDocumentType() );
        userDocumentResponse.fileName( document.getFileName() );
        userDocumentResponse.fileSizeBytes( document.getFileSizeBytes() );
        userDocumentResponse.fileUrl( document.getFileUrl() );
        userDocumentResponse.id( document.getId() );
        userDocumentResponse.mimeType( document.getMimeType() );
        userDocumentResponse.rejectionReason( document.getRejectionReason() );
        userDocumentResponse.uploadedAt( document.getUploadedAt() );
        userDocumentResponse.verificationStatus( document.getVerificationStatus() );
        userDocumentResponse.verifiedAt( document.getVerifiedAt() );

        return userDocumentResponse.build();
    }
}
