package xyz.inv1s1bl3.countries.database.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Country entity representing a country in the database
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class Country {
    
    private Integer id;
    private String name;
    private String displayName;
    private String description;
    private String flagDescription;
    private String governmentType;
    private UUID leaderUuid;
    private double treasuryBalance;
    private double taxRate;
    private LocalDateTime createdAt;
    private String capitalWorld;
    private Integer capitalX;
    private Integer capitalZ;
    private boolean isActive;
    
    /**
     * Check if country has a capital set
     * @return true if capital is set
     */
    public boolean hasCapital() {
        return this.capitalWorld != null && this.capitalX != null && this.capitalZ != null;
    }
    
    /**
     * Set the capital location
     * @param world World name
     * @param x X coordinate
     * @param z Z coordinate
     */
    public void setCapital(final String world, final int x, final int z) {
        this.capitalWorld = world;
        this.capitalX = x;
        this.capitalZ = z;
    }
    
    /**
     * Clear the capital location
     */
    public void clearCapital() {
        this.capitalWorld = null;
        this.capitalX = null;
        this.capitalZ = null;
    }
    
    /**
     * Check if player is the leader of this country
     * @param playerUuid Player UUID to check
     * @return true if player is the leader
     */
    public boolean isLeader(final UUID playerUuid) {
        return this.leaderUuid != null && this.leaderUuid.equals(playerUuid);
    }
    
    /**
     * Add money to the treasury
     * @param amount Amount to add
     */
    public void addToTreasury(final double amount) {
        this.treasuryBalance += amount;
    }
    
    /**
     * Remove money from the treasury
     * @param amount Amount to remove
     * @return true if successful, false if insufficient funds
     */
    public boolean removeFromTreasury(final double amount) {
        if (this.treasuryBalance >= amount) {
            this.treasuryBalance -= amount;
            return true;
        }
        return false;
    }
    
    /**
     * Check if treasury has sufficient funds
     * @param amount Amount to check
     * @return true if treasury has enough money
     */
    public boolean hasSufficientFunds(final double amount) {
        return this.treasuryBalance >= amount;
    }
    
    /**
     * Set tax rate with validation
     * @param rate Tax rate (0-100)
     */
    public void setTaxRate(final double rate) {
        this.taxRate = Math.max(0.0, Math.min(100.0, rate));
    }
    
    /**
     * Get formatted display name
     * @return Display name or name if display name is null
     */
    public String getFormattedName() {
        return this.displayName != null && !this.displayName.isEmpty() ? this.displayName : this.name;
    }
}