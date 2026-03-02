package com.insurance.auth.mapper;

import com.insurance.auth.dto.response.UserResponse;
import com.insurance.auth.entity.Role;
import com.insurance.auth.entity.User;
import org.mapstruct.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MapStruct Mapper — converts between User entity and UserResponse DTO.
 * MapStruct generates the implementation at compile time (zero reflection overhead).
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", expression = "java(mapRoles(user.getRoles()))")
    UserResponse toUserResponse(User user);

    default List<String> mapRoles(Set<Role> roles) {
        if (roles == null) return List.of();
        return roles.stream()
            .map(role -> role.getRoleName().name())
            .collect(Collectors.toList());
    }
}
