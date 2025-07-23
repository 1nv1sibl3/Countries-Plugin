package xyz.inv1s1bl3.countries.core.law;

/**
 * Represents different types of crimes that can be committed.
 */
public enum CrimeType {
    THEFT("Theft", "Stealing from another player", 1, 100.0, 5),
    VANDALISM("Vandalism", "Destroying property without permission", 1, 50.0, 3),
    TRESPASSING("Trespassing", "Entering restricted territory", 1, 25.0, 2),
    ASSAULT("Assault", "Attacking another player", 2, 200.0, 10),
    MURDER("Murder", "Killing another player", 3, 500.0, 30),
    FRAUD("Fraud", "Economic deception or scamming", 2, 300.0, 15),
    TREASON("Treason", "Acting against one's own country", 3, 1000.0, 60),
    SMUGGLING("Smuggling", "Illegal transportation of goods", 2, 150.0, 8),
    ESPIONAGE("Espionage", "Spying on another country", 2, 250.0, 12),
    REBELLION("Rebellion", "Organizing against lawful authority", 3, 750.0, 45);
    
    private final String displayName;
    private final String description;
    private final int severity; // 1-3 (minor, major, severe)
    private final double baseFine;
    private final int baseJailTime; // in minutes
    
    CrimeType(String displayName, String description, int severity, double baseFine, int baseJailTime) {
        this.displayName = displayName;
        this.description = description;
        this.severity = severity;
        this.baseFine = baseFine;
        this.baseJailTime = baseJailTime;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getSeverity() {
        return severity;
    }
    
    public double getBaseFine() {
        return baseFine;
    }
    
    public int getBaseJailTime() {
        return baseJailTime;
    }
    
    /**
     * Get crime type from string, case insensitive
     */
    public static CrimeType fromString(String name) {
        for (CrimeType type : values()) {
            if (type.name().equalsIgnoreCase(name) || 
                type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return THEFT; // Default
    }
    
    /**
     * Get severity color code
     */
    public String getSeverityColor() {
        return switch (severity) {
            case 1 -> "&e"; // Yellow for minor
            case 2 -> "&6"; // Orange for major
            case 3 -> "&c"; // Red for severe
            default -> "&7"; // Gray for unknown
        };
    }
    
    /**
     * Get severity display name
     */
    public String getSeverityName() {
        return switch (severity) {
            case 1 -> "Minor";
            case 2 -> "Major";
            case 3 -> "Severe";
            default -> "Unknown";
        };
    }
}