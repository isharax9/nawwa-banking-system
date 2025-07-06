package lk.banking.core.entity;

import jakarta.persistence.*;
import lk.banking.core.entity.enums.UserRole;

@Entity
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, unique=true)
    private UserRole name;

    // Constructors, getters, setters
    public Role() {}

    public Role(UserRole name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public UserRole getName() { return name; }
    public void setName(UserRole name) { this.name = name; }
}