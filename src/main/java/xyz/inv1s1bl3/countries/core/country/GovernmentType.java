package xyz.inv1s1bl3.countries.core.country;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the different types of government a country can have.
 * Each government type affects how the country operates and makes decisions.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
@Getter
public enum GovernmentType {
    
    DEMOCRACY("Democracy", "Citizens vote on major decisions", "&a"),
    MONARCHY("Monarchy", "Leader has absolute power", "&6"),
    REPUBLIC("Republic", "Elected officials make decisions", "&b"),
    OLIGARCHY("Oligarchy", "Small group of leaders rule", "&d"),
    FEDERATION("Federation", "Autonomous regions with central government", "&e"),
    ANARCHY("Anarchy", "No formal government structure", "&c");
    
    private final String displayName;
    private final String description;
    private final String colorCode;
    
    GovernmentType(@NotNull final String displayName, 
                   @NotNull final String description, 
                   @NotNull final String colorCode) {
        this.displayName = displayName;
        this.description = description;
        this.colorCode = colorCode;
    }
    
    /**
     * Get the formatted display name with color.
     * 
     * @return the formatted display name
     */
    @NotNull
    public String getFormattedName() {
        return this.colorCode + this.displayName;
    }
    
    /**
     * Get the formatted description with color.
     * 
     * @return the formatted description
     */
    @NotNull
    public String getFormattedDescription() {
        return "&7" + this.description;
    }
    
    /**
     * Get a government type by its display name.
     * 
     * @param name the display name
     * @return the government type, or DEMOCRACY if not found
     */
    @NotNull
    public static GovernmentType fromString(@NotNull final String name) {
        for (final GovernmentType type : values()) {
            if (type.displayName.equalsIgnoreCase(name) || type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return DEMOCRACY; // Default to democracy if not found
    }
    
    /**
     * Check if this government type allows voting.
     * 
     * @return true if voting is allowed
     */
    public boolean allowsVoting() {
        return this == DEMOCRACY || this == REPUBLIC || this == FEDERATION;
    }
    
    /**
     * Check if this government type has a single leader.
     * 
     * @return true if there's a single leader
     */
    public boolean hasSingleLeader() {
        return this == MONARCHY;
    }
    
    /**
     * Check if this government type allows multiple leaders.
     * 
     * @return true if multiple leaders are allowed
     */
    public boolean allowsMultipleLeaders() {
        return this == OLIGARCHY || this == FEDERATION;
    }
    
    /**
     * Get the maximum number of leaders for this government type.
     * 
     * @return the maximum number of leaders
     */
    public int getMaxLeaders() {
        switch (this) {
            case MONARCHY:
                return 1;
            case OLIGARCHY:
                return 5;
            case FEDERATION:
                return 10;
            case ANARCHY:
                return 0;
            case DEMOCRACY:
            case REPUBLIC:
            default:
                return 1;
        }
    }
    
    /**
     * Get the tax collection efficiency for this government type.
     * 
     * @return the efficiency multiplier (0.0 to 1.0)
     */
    public double getTaxEfficiency() {
        switch (this) {
            case MONARCHY:
                return 0.95;
            case REPUBLIC:
                return 0.90;
            case DEMOCRACY:
                return 0.85;
            case FEDERATION:
                return 0.80;
            case OLIGARCHY:
                return 0.75;
            case ANARCHY:
                return 0.50;
            default:
                return 0.85;
        }
    }
}