package xyz.inv1s1bl3.countries.core.economy;

/**
 * Represents different types of economic transactions.
 */
public enum TransactionType {
    TRANSFER("Transfer", "Money transfer between accounts"),
    TAX_COLLECTION("Tax Collection", "Tax collected from citizens"),
    SALARY_PAYMENT("Salary Payment", "Salary paid to citizens"),
    TERRITORY_CLAIM("Territory Claim", "Cost for claiming territory"),
    TERRITORY_UPKEEP("Territory Upkeep", "Daily territory maintenance cost"),
    COUNTRY_CREATION("Country Creation", "Cost for creating a country"),
    ALLIANCE_FEE("Alliance Fee", "Cost for forming an alliance"),
    WAR_DECLARATION("War Declaration", "Cost for declaring war"),
    FINE_PAYMENT("Fine Payment", "Payment of legal fine"),
    TRADE_PAYMENT("Trade Payment", "Payment for trade agreement"),
    DEPOSIT("Deposit", "Money deposited into account"),
    WITHDRAWAL("Withdrawal", "Money withdrawn from account"),
    SYSTEM_ADJUSTMENT("System Adjustment", "Administrative balance adjustment");
    
    private final String displayName;
    private final String description;
    
    TransactionType(String displayName, String description) {
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
     * Get transaction type from string, case insensitive
     */
    public static TransactionType fromString(String name) {
        for (TransactionType type : values()) {
            if (type.name().equalsIgnoreCase(name) || 
                type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return TRANSFER; // Default
    }
}