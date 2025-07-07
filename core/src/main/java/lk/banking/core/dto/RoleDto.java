package lk.banking.core.dto;

import lk.banking.core.entity.enums.UserRole;

/**
 * Data Transfer Object for Role entity.
 */
public class RoleDto {
    private Long id;
    private UserRole name;

    public RoleDto() {}
    public RoleDto(Long id, UserRole name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserRole getName() { return name; }
    public void setName(UserRole name) { this.name = name; }
}