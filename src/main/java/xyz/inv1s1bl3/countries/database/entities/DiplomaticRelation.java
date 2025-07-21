package xyz.inv1s1bl3.countries.database.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Diplomatic relation entity representing relations between countries
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class DiplomaticRelation {
    
    private Integer id;
    private Integer country1Id;
    private Integer country2Id;
    private String relationType;
    private LocalDateTime establishedAt;
    private UUID establishedByUuid;
    private LocalDateTime lastChangedAt;
    private UUID lastChangedByUuid;
    private String notes;
    
    /**
     * Check if relation involves a specific country
     * @param countryId Country ID to check
     * @return true if country is involved
     */
    public boolean involvesCountry(final Integer countryId) {
        return this.country1Id.equals(countryId) || this.country2Id.equals(countryId);
    }
    
    /**
     * Get the other country in the relation
     * @param countryId One country ID
     * @return The other country ID, or null if country not involved
     */
    public Integer getOtherCountry(final Integer countryId) {
        if (this.country1Id.equals(countryId)) {
            return this.country2Id;
        } else if (this.country2Id.equals(countryId)) {
            return this.country1Id;
        }
        return null;
    }
    
    /**
     * Check if relation is of a specific type
     * @param type Relation type to check
     * @return true if types match
     */
    public boolean isType(final String type) {
        return this.relationType != null && this.relationType.equalsIgnoreCase(type);
    }
    
    /**
     * Check if countries are allies
     * @return true if relation is ally
     */
    public boolean isAlly() {
        return this.isType("ally");
    }
    
    /**
     * Check if countries are friendly
     * @return true if relation is friendly
     */
    public boolean isFriendly() {
        return this.isType("friendly");
    }
    
    /**
     * Check if countries are neutral
     * @return true if relation is neutral
     */
    public boolean isNeutral() {
        return this.isType("neutral");
    }
    
    /**
     * Check if countries are unfriendly
     * @return true if relation is unfriendly
     */
    public boolean isUnfriendly() {
        return this.isType("unfriendly");
    }
    
    /**
     * Check if countries are enemies
     * @return true if relation is enemy
     */
    public boolean isEnemy() {
        return this.isType("enemy");
    }
    
    /**
     * Check if relation is positive (ally or friendly)
     * @return true if positive relation
     */
    public boolean isPositive() {
        return this.isAlly() || this.isFriendly();
    }
    
    /**
     * Check if relation is negative (unfriendly or enemy)
     * @return true if negative relation
     */
    public boolean isNegative() {
        return this.isUnfriendly() || this.isEnemy();
    }
    
    /**
     * Update the relation type and timestamp
     * @param newType New relation type
     * @param changedByUuid UUID of player making the change
     */
    public void updateRelation(final String newType, final UUID changedByUuid) {
        this.relationType = newType;
        this.lastChangedAt = LocalDateTime.now();
        this.lastChangedByUuid = changedByUuid;
    }
}