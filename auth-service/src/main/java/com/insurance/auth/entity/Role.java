package com.insurance.auth.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false, unique = true, length = 30)
    private RoleName roleName;

    public enum RoleName {
        ROLE_ADMIN,       // Full system access
        ROLE_AGENT,       // Insurance agent — manages claims
        ROLE_CUSTOMER,    // End customer — submits & views own claims
        ROLE_AUDITOR      // Read-only access for compliance
    }
}
