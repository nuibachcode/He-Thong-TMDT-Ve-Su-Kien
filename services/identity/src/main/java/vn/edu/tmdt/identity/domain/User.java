package vn.edu.tmdt.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 160)
    public String email;

    @Column(nullable = false, length = 120)
    public String password;

    @Column(nullable = false, length = 160)
    public String fullName;

    @Column(length = 32)
    public String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Role role = Role.CUSTOMER;

    @Column(nullable = false)
    public boolean enabled = true;

    @Column(nullable = false)
    public Instant createdAt;

    @Column(length = 500)
    public String interests;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public Role getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
