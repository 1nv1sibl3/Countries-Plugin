package xyz.inv1s1bl3.countries.database.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transaction entity representing a financial transaction in the database
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class Transaction {
    
    private Integer id;
    private String transactionType;
    private UUID fromPlayerUuid;
    private UUID toPlayerUuid;
    private Integer fromCountryId;
    private Integer toCountryId;
    private double amount;
    private String description;
    private String category;
    private LocalDateTime createdAt;
    private String worldName;
    private Integer x;
    private Integer y;
    private Integer z;
    
    /**
     * Check if transaction involves a specific player
     * @param playerUuid Player UUID to check
     * @return true if player is involved
     */
    public boolean involvesPlayer(final UUID playerUuid) {
        return (this.fromPlayerUuid != null && this.fromPlayerUuid.equals(playerUuid)) ||
               (this.toPlayerUuid != null && this.toPlayerUuid.equals(playerUuid));
    }
    
    /**
     * Check if transaction involves a specific country
     * @param countryId Country ID to check
     * @return true if country is involved
     */
    public boolean involvesCountry(final Integer countryId) {
        return (this.fromCountryId != null && this.fromCountryId.equals(countryId)) ||
               (this.toCountryId != null && this.toCountryId.equals(countryId));
    }
    
    /**
     * Check if this is a player-to-player transaction
     * @return true if both from and to are players
     */
    public boolean isPlayerToPlayer() {
        return this.fromPlayerUuid != null && this.toPlayerUuid != null;
    }
    
    /**
     * Check if this is a player-to-country transaction
     * @return true if from player to country
     */
    public boolean isPlayerToCountry() {
        return this.fromPlayerUuid != null && this.toCountryId != null;
    }
    
    /**
     * Check if this is a country-to-player transaction
     * @return true if from country to player
     */
    public boolean isCountryToPlayer() {
        return this.fromCountryId != null && this.toPlayerUuid != null;
    }
    
    /**
     * Check if this is a country-to-country transaction
     * @return true if both from and to are countries
     */
    public boolean isCountryToCountry() {
        return this.fromCountryId != null && this.toCountryId != null;
    }
    
    /**
     * Check if transaction has location data
     * @return true if location is set
     */
    public boolean hasLocation() {
        return this.worldName != null && this.x != null && this.y != null && this.z != null;
    }
    
    /**
     * Get formatted location string
     * @return Formatted location or "Unknown" if no location
     */
    public String getFormattedLocation() {
        if (this.hasLocation()) {
            return String.format("%s (%d, %d, %d)", this.worldName, this.x, this.y, this.z);
        }
        return "Unknown";
    }
    
    /**
     * Check if transaction is of a specific type
     * @param type Transaction type to check
     * @return true if types match
     */
    public boolean isType(final String type) {
        return this.transactionType != null && this.transactionType.equalsIgnoreCase(type);
    }
    
    /**
     * Check if transaction is in a specific category
     * @param category Category to check
     * @return true if categories match
     */
    public boolean isCategory(final String category) {
        return this.category != null && this.category.equalsIgnoreCase(category);
    }
}