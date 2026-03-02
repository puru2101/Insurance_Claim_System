package com.insurance.auth.mapper;

import com.insurance.auth.dto.response.UserResponse;
import com.insurance.auth.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-02T08:21:00+0530",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.37.0.v20240215-1558, environment: Java 17.0.11 (Eclipse Adoptium)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserResponse toUserResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UserResponse.UserResponseBuilder userResponse = UserResponse.builder();

        userResponse.createdAt( user.getCreatedAt() );
        userResponse.email( user.getEmail() );
        userResponse.firstName( user.getFirstName() );
        userResponse.id( user.getId() );
        userResponse.isActive( user.getIsActive() );
        userResponse.isEmailVerified( user.getIsEmailVerified() );
        userResponse.lastLoginAt( user.getLastLoginAt() );
        userResponse.lastName( user.getLastName() );
        userResponse.phoneNumber( user.getPhoneNumber() );
        userResponse.username( user.getUsername() );

        userResponse.roles( mapRoles(user.getRoles()) );

        return userResponse.build();
    }
}
