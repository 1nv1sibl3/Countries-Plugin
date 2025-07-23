package xyz.inv1s1bl3.countries.core.country;

/**
 * Represents different types of government systems for countries.
 */
public enum GovernmentType {
    MONARCHY("Monarchy", "A single ruler with absolute power"),
    DEMOCRACY("Democracy", "Citizens vote on major decisions"),
    REPUBLIC("Republic", "Elected representatives make decisions"),
    OLIGARCHY("Oligarchy", "Small group of leaders rule"),
    ANARCHY("Anarchy", "No formal government structure"),
    THEOCRACY("Theocracy", "Religious leaders govern"),
    FEDERATION("Federation", "Multiple regions with shared governance");
    
    private final String displayName;
    private final String description;
    
    GovernmentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get government type from string, case insensitive
     */
    public static GovernmentType fromString(String name) {
        for (GovernmentType type : values()) {
            if (type.name().equalsIgnoreCase(name) || 
                type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return MONARCHY; // Default
    }
}