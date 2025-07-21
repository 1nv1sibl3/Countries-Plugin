package xyz.inv1s1bl3.countries.database.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Player entity representing a player in the database
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class Player {
    
    private UUID uuid;
    private String username;
    private Integer countryId;
    private String role;
    private double balance;
    private LocalDateTime joinedAt;
    private LocalDateTime lastSeen;
    private boolean isOnline;
    
    /**
     * Check if player is a member of a country
     * @return true if player belongs to a country
     */
    public boolean hasCountry() {
        return this.countryId != null;
    }
    
    /**
     * Check if player has a specific role
     * @param role Role to check
     * @return true if player has the role
     */
    public boolean hasRole(final String role) {
        return this.role != null && this.role.equalsIgnoreCase(role);
    }
    
    /**
     * Check if player is a leader
     * @return true if player is a leader
     */
    public boolean isLeader() {
        return this.hasRole("leader") || this.hasRole("monarch") || 
               this.hasRole("president") || this.hasRole("consul") || 
               this.hasRole("chancellor") || this.hasRole("oligarch") || 
               this.hasRole("high_priest") || this.hasRole("autocrat");
    }
    
    /**
     * Check if player has administrative permissions
     * @return true if player has admin permissions
     */
    public boolean hasAdminPermissions() {
        return this.isLeader() || this.hasRole("minister") || 
               this.hasRole("noble") || this.hasRole("senator") || 
               this.hasRole("governor") || this.hasRole("elite") || 
               this.hasRole("priest") || this.hasRole("lieutenant");
    }
    
    /**
     * Update player's last seen timestamp
     */
    public void updateLastSeen() {
        this.lastSeen = LocalDateTime.now();
    }
    
    /**
     * Set player online status
     * @param online Online status
     */
    public void setOnlineStatus(final boolean online) {
        this.isOnline = online;
        if (online) {
            this.updateLastSeen();
        }
    }
}