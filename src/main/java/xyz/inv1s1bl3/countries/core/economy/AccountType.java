package xyz.inv1s1bl3.countries.core.economy;

/**
 * Represents different types of economy accounts.
 */
public enum AccountType {
    PLAYER("Player", "Personal player account"),
    COUNTRY("Country", "Country treasury account"),
    BANK("Bank", "Central bank account"),
    SYSTEM("System", "System account for transactions");
    
    private final String displayName;
    private final String description;
    
    AccountType(String displayName, String description) {
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
     * Get account type from string, case insensitive
     */
    public static AccountType fromString(String name) {
        for (AccountType type : values()) {
            if (type.name().equalsIgnoreCase(name) || 
                type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return PLAYER; // Default
    }
}