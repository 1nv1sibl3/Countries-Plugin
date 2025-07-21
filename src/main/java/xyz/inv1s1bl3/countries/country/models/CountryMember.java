package xyz.inv1s1bl3.countries.country.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents a country member with role and permissions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class CountryMember {
    
    private Integer id;
    private Integer countryId;
    private UUID playerUuid;
    private String role;
    private LocalDateTime joinedAt;
    private double salary;
    private List<String> permissions;
    
    /**
     * Check if member has a specific permission
     * @param permission Permission to check
     * @return true if member has the permission
     */
    public boolean hasPermission(final String permission) {
        return this.permissions != null && this.permissions.contains(permission);
    }
    
    /**
     * Add a permission to the member
     * @param permission Permission to add
     */
    public void addPermission(final String permission) {
        if (this.permissions != null && !this.permissions.contains(permission)) {
            this.permissions.add(permission);
        }
    }
    
    /**
     * Remove a permission from the member
     * @param permission Permission to remove
     */
    public void removePermission(final String permission) {
        if (this.permissions != null) {
            this.permissions.remove(permission);
        }
    }
    
    /**
     * Check if member has a specific role
     * @param role Role to check
     * @return true if member has the role
     */
    public boolean hasRole(final String role) {
        return this.role != null && this.role.equalsIgnoreCase(role);
    }
    
    /**
     * Check if member is a leader
     * @return true if member is a leader
     */
    public boolean isLeader() {
        return this.hasRole("leader") || this.hasRole("monarch") || 
               this.hasRole("president") || this.hasRole("consul") || 
               this.hasRole("chancellor") || this.hasRole("oligarch") || 
               this.hasRole("high_priest") || this.hasRole("autocrat");
    }
    
    /**
     * Check if member has administrative permissions
     * @return true if member has admin permissions
     */
    public boolean hasAdminPermissions() {
        return this.isLeader() || this.hasRole("minister") || 
               this.hasRole("noble") || this.hasRole("senator") || 
               this.hasRole("governor") || this.hasRole("elite") || 
               this.hasRole("priest") || this.hasRole("lieutenant");
    }
    
    /**
     * Get formatted role name
     * @return Formatted role name
     */
    public String getFormattedRole() {
        if (this.role == null) {
            return "Unknown";
        }
        
        return this.role.substring(0, 1).toUpperCase() + this.role.substring(1).toLowerCase();
    }
}