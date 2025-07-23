package xyz.inv1s1bl3.countries.core.diplomacy;

/**
 * Represents different types of diplomatic relations between countries.
 */
public enum RelationType {
    ALLIED("Allied", "Friendly alliance with mutual benefits", true, true, true),
    FRIENDLY("Friendly", "Positive relations with some cooperation", true, true, false),
    NEUTRAL("Neutral", "No special relationship", false, false, false),
    HOSTILE("Hostile", "Negative relations with restrictions", false, false, false),
    AT_WAR("At War", "Active state of war", false, false, false),
    TRADE_PARTNER("Trade Partner", "Special trade relationship", true, true, true),
    VASSAL("Vassal", "Subordinate relationship", false, true, false),
    OVERLORD("Overlord", "Dominant relationship", true, false, false);
    
    private final String displayName;
    private final String description;
    private final boolean allowsTravel;
    private final boolean allowsTrade;
    private final boolean allowsAlliance;
    
    RelationType(String displayName, String description, boolean allowsTravel, 
                boolean allowsTrade, boolean allowsAlliance) {
        this.displayName = displayName;
        this.description = description;
        this.allowsTravel = allowsTravel;
        this.allowsTrade = allowsTrade;
        this.allowsAlliance = allowsAlliance;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean allowsTravel() {
        return allowsTravel;
    }
    
    public boolean allowsTrade() {
        return allowsTrade;
    }
    
    public boolean allowsAlliance() {
        return allowsAlliance;
    }
    
    /**
     * Get relation type from string, case insensitive
     */
    public static RelationType fromString(String name) {
        for (RelationType type : values()) {
            if (type.name().equalsIgnoreCase(name) || 
                type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return NEUTRAL; // Default
    }
}