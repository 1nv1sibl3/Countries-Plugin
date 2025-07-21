package xyz.inv1s1bl3.countries.core.territory;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the different types of territories a country can have.
 * Each type has different characteristics and costs.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
@Getter
public enum TerritoryType {
    
    CAPITAL("Capital", "The main city and seat of government", "&6", 1000.0, 50.0, true, true),
    CITY("City", "A major urban settlement", "&e", 500.0, 25.0, true, true),
    TOWN("Town", "A smaller urban settlement", "&a", 250.0, 15.0, true, false),
    VILLAGE("Village", "A small rural settlement", "&f", 100.0, 10.0, false, false),
    OUTPOST("Outpost", "A remote military or trading post", "&c", 200.0, 20.0, false, true),
    FARM("Farm", "Agricultural land for food production", "&2", 150.0, 5.0, false, false),
    MINE("Mine", "Resource extraction site", "&8", 300.0, 30.0, false, true),
    FORTRESS("Fortress", "Heavily fortified military installation", "&4", 800.0, 40.0, false, true),
    WILDERNESS("Wilderness", "Unclaimed natural land", "&7", 50.0, 2.0, false, false);
    
    private final String displayName;
    private final String description;
    private final String colorCode;
    private final double claimCost;
    private final double maintenanceCost;
    private final boolean allowsResidency;
    private final boolean allowsMilitary;
    
    TerritoryType(@NotNull final String displayName,
                  @NotNull final String description,
                  @NotNull final String colorCode,
                  final double claimCost,
                  final double maintenanceCost,
                  final boolean allowsResidency,
                  final boolean allowsMilitary) {
        this.displayName = displayName;
        this.description = description;
        this.colorCode = colorCode;
        this.claimCost = claimCost;
        this.maintenanceCost = maintenanceCost;
        this.allowsResidency = allowsResidency;
        this.allowsMilitary = allowsMilitary;
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
     * Get a territory type by its display name.
     * 
     * @param name the display name
     * @return the territory type, or WILDERNESS if not found
     */
    @NotNull
    public static TerritoryType fromString(@NotNull final String name) {
        for (final TerritoryType type : values()) {
            if (type.displayName.equalsIgnoreCase(name) || type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return WILDERNESS; // Default to wilderness if not found
    }
    
    /**
     * Check if this territory type can be upgraded to another type.
     * 
     * @param targetType the target type
     * @return true if upgrade is possible
     */
    public boolean canUpgradeTo(@NotNull final TerritoryType targetType) {
        // Define upgrade paths
        switch (this) {
            case WILDERNESS:
                return targetType == VILLAGE || targetType == FARM || targetType == OUTPOST;
            case VILLAGE:
                return targetType == TOWN || targetType == FARM;
            case TOWN:
                return targetType == CITY;
            case CITY:
                return targetType == CAPITAL;
            case FARM:
                return targetType == VILLAGE;
            case OUTPOST:
                return targetType == FORTRESS || targetType == TOWN;
            case MINE:
                return targetType == OUTPOST;
            case FORTRESS:
                return false; // Fortress is a terminal type
            case CAPITAL:
                return false; // Capital is the highest type
            default:
                return false;
        }
    }
    
    /**
     * Get the upgrade cost to another territory type.
     * 
     * @param targetType the target type
     * @return the upgrade cost, or -1 if upgrade not possible
     */
    public double getUpgradeCost(@NotNull final TerritoryType targetType) {
        if (!this.canUpgradeTo(targetType)) {
            return -1.0;
        }
        
        return targetType.claimCost - this.claimCost;
    }
    
    /**
     * Get the maximum number of chunks for this territory type.
     * 
     * @return the maximum chunk count
     */
    public int getMaxChunks() {
        switch (this) {
            case CAPITAL:
                return 200;
            case CITY:
                return 150;
            case TOWN:
                return 100;
            case VILLAGE:
                return 50;
            case FORTRESS:
                return 75;
            case OUTPOST:
                return 25;
            case MINE:
                return 40;
            case FARM:
                return 80;
            case WILDERNESS:
                return 20;
            default:
                return 50;
        }
    }
    
    /**
     * Get the population capacity for this territory type.
     * 
     * @return the population capacity
     */
    public int getPopulationCapacity() {
        switch (this) {
            case CAPITAL:
                return 500;
            case CITY:
                return 200;
            case TOWN:
                return 100;
            case VILLAGE:
                return 50;
            case FORTRESS:
                return 75;
            case OUTPOST:
                return 25;
            case MINE:
                return 30;
            case FARM:
                return 20;
            case WILDERNESS:
                return 10;
            default:
                return 50;
        }
    }
    
    /**
     * Get the defense bonus for this territory type.
     * 
     * @return the defense bonus multiplier
     */
    public double getDefenseBonus() {
        switch (this) {
            case FORTRESS:
                return 2.0;
            case CAPITAL:
                return 1.5;
            case CITY:
                return 1.3;
            case OUTPOST:
                return 1.4;
            case TOWN:
                return 1.2;
            case VILLAGE:
                return 1.1;
            case MINE:
                return 1.1;
            case FARM:
                return 1.0;
            case WILDERNESS:
                return 0.8;
            default:
                return 1.0;
        }
    }
}