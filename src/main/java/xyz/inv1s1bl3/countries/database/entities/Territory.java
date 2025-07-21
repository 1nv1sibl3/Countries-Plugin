package xyz.inv1s1bl3.countries.database.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Territory entity representing a claimed chunk in the database
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class Territory {
    
    private Integer id;
    private Integer countryId;
    private String worldName;
    private Integer chunkX;
    private Integer chunkZ;
    private String territoryType;
    private LocalDateTime claimedAt;
    private UUID claimedByUuid;
    private double claimCost;
    private double maintenanceCost;
    private LocalDateTime lastMaintenancePaid;
    private String protectionFlags;
    
    /**
     * Get the chunk coordinate as a string
     * @return Formatted chunk coordinates
     */
    public String getChunkCoordinates() {
        return this.chunkX + "," + this.chunkZ;
    }
    
    /**
     * Get the world coordinates of the chunk center
     * @return Array of [x, z] world coordinates
     */
    public int[] getWorldCoordinates() {
        return new int[]{this.chunkX * 16 + 8, this.chunkZ * 16 + 8};
    }
    
    /**
     * Check if territory type matches
     * @param type Territory type to check
     * @return true if types match
     */
    public boolean isType(final String type) {
        return this.territoryType != null && this.territoryType.equalsIgnoreCase(type);
    }
    
    /**
     * Check if territory is residential
     * @return true if residential
     */
    public boolean isResidential() {
        return this.isType("residential");
    }
    
    /**
     * Check if territory is commercial
     * @return true if commercial
     */
    public boolean isCommercial() {
        return this.isType("commercial");
    }
    
    /**
     * Check if territory is industrial
     * @return true if industrial
     */
    public boolean isIndustrial() {
        return this.isType("industrial");
    }
    
    /**
     * Check if territory is agricultural
     * @return true if agricultural
     */
    public boolean isAgricultural() {
        return this.isType("agricultural");
    }
    
    /**
     * Check if territory is military
     * @return true if military
     */
    public boolean isMilitary() {
        return this.isType("military");
    }
    
    /**
     * Check if territory is capital
     * @return true if capital
     */
    public boolean isCapital() {
        return this.isType("capital");
    }
    
    /**
     * Check if maintenance is due
     * @param maintenanceInterval Maintenance interval in hours
     * @return true if maintenance is due
     */
    public boolean isMaintenanceDue(final int maintenanceInterval) {
        if (this.lastMaintenancePaid == null) {
            return true;
        }
        
        final LocalDateTime nextPayment = this.lastMaintenancePaid.plusHours(maintenanceInterval);
        return LocalDateTime.now().isAfter(nextPayment);
    }
    
    /**
     * Update maintenance payment timestamp
     */
    public void payMaintenance() {
        this.lastMaintenancePaid = LocalDateTime.now();
    }
    
    /**
     * Check if territory was claimed by a specific player
     * @param playerUuid Player UUID to check
     * @return true if claimed by the player
     */
    public boolean wasClaimedBy(final UUID playerUuid) {
        return this.claimedByUuid != null && this.claimedByUuid.equals(playerUuid);
    }
}