package com.preplate.user.entity;

import com.preplate.common.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "registration_number", nullable = false, unique = true, length = 30)
    private String registrationNumber;
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;
    private String department;
    @Column(name = "hostel_name")
    private String hostelName;
    @Column(name = "hostel_room")
    private String hostelRoom;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected User() {
    }

    public User(String registrationNumber, String fullName, String passwordHash, Role role,
            String department, String hostelName, String hostelRoom) {
        this.registrationNumber = registrationNumber;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.department = department;
        this.hostelName = hostelName;
        this.hostelRoom = hostelRoom;
    }

    public Long getId() { return id; }
    public String getRegistrationNumber() { return registrationNumber; }
    public String getFullName() { return fullName; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public String getDepartment() { return department; }
    public String getHostelName() { return hostelName; }
    public String getHostelRoom() { return hostelRoom; }
}
