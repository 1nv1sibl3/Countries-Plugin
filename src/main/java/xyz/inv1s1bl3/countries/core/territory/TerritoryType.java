package xyz.inv1s1bl3.countries.core.territory;

/**
 * Represents different types of territories with specific properties and restrictions.
 */
public enum TerritoryType {
    RESIDENTIAL("Residential", "Housing and civilian areas", true, true, false),
    COMMERCIAL("Commercial", "Business and trade districts", true, true, true),
    INDUSTRIAL("Industrial", "Manufacturing and production areas", true, false, true),
    MILITARY("Military", "Defense and strategic locations", false, false, false),
    AGRICULTURAL("Agricultural", "Farming and food production", true, true, true),
    RECREATIONAL("Recreational", "Parks and entertainment areas", true, true, false),
    GOVERNMENT("Government", "Administrative and official buildings", false, false, false),
    WILDERNESS("Wilderness", "Natural and undeveloped land", true, false, false);
    
    private final String displayName;
    private final String description;
    private final boolean allowPublicAccess;
    private final boolean allowBuilding;
    private final boolean allowTrade;
    
    TerritoryType(String displayName, String description, boolean allowPublicAccess, 
                  boolean allowBuilding, boolean allowTrade) {
        this.displayName = displayName;
        this.description = description;
        this.allowPublicAccess = allowPublicAccess;
        this.allowBuilding = allowBuilding;
        this.allowTrade = allowTrade;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean allowsPublicAccess() {
        return allowPublicAccess;
    }
    
    public boolean allowsBuilding() {
        return allowBuilding;
    }
    
    public boolean allowsTrade() {
        return allowTrade;
    }
    
    /**
     * Get territory type from string, case insensitive
     */
    public static TerritoryType fromString(String name) {
        for (TerritoryType type : values()) {
            if (type.name().equalsIgnoreCase(name) || 
                type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return RESIDENTIAL; // Default
    }
}