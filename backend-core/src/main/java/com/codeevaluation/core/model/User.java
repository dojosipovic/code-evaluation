package com.codeevaluation.core.model;

import com.codeevaluation.core.enumeration.Role;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "app_user")
@Getter
@Setter
public class User extends PanacheEntityBase {

    public static final int USERNAME_MAX_LENGTH = 100;
    public static final int FIRSTNAME_MAX_LENGTH = 30;
    public static final int LASTNAME_MAX_LENGTH = 40;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = USERNAME_MAX_LENGTH)
    private String username;

    @Column(nullable = false, length = FIRSTNAME_MAX_LENGTH)
    private String firstname;

    @Column(nullable = false, length = LASTNAME_MAX_LENGTH)
    private String lastname;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private Boolean enabled;

    @OneToMany(mappedBy = "user")
    private List<GroupMember> groupMemberships;

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public List<Group> getGroups() {
        return groupMemberships.stream()
                .map(GroupMember::getGroup)
                .toList();
    }
}
