package xyz.inv1s1bl3.countries.core.economy;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the different types of financial transactions.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
@Getter
public enum TransactionType {
    
    DEPOSIT("Deposit", "&a+", "Money added to account"),
    WITHDRAWAL("Withdrawal", "&c-", "Money removed from account"),
    TRANSFER("Transfer", "&e→", "Money transferred between accounts"),
    TAX_COLLECTION("Tax Collection", "&6T", "Tax collected from citizens"),
    SALARY_PAYMENT("Salary Payment", "&bS", "Salary paid to citizen"),
    TERRITORY_COST("Territory Cost", "&dL", "Cost for territory operations"),
    MARKET_TRANSACTION("Market Transaction", "&eM", "Market purchase or sale"),
    FINE_PAYMENT("Fine Payment", "&cF", "Fine paid for law violation"),
    ALLIANCE_COST("Alliance Cost", "&9A", "Cost for diplomatic alliance"),
    WAR_COST("War Cost", "&4W", "Cost for war declaration"),
    MAINTENANCE("Maintenance", "&7M", "Regular maintenance cost"),
    BONUS("Bonus", "&a★", "Special bonus payment"),
    REFUND("Refund", "&2R", "Money refunded");
    
    private final String displayName;
    private final String symbol;
    private final String description;
    
    TransactionType(@NotNull final String displayName, 
                   @NotNull final String symbol, 
                   @NotNull final String description) {
        this.displayName = displayName;
        this.symbol = symbol;
        this.description = description;
    }
    
    /**
     * Get the formatted display name with symbol.
     * 
     * @return the formatted display name
     */
    @NotNull
    public String getFormattedName() {
        return this.symbol + " " + this.displayName;
    }
    
    /**
     * Get a transaction type by its display name.
     * 
     * @param name the display name
     * @return the transaction type, or DEPOSIT if not found
     */
    @NotNull
    public static TransactionType fromString(@NotNull final String name) {
        for (final TransactionType type : values()) {
            if (type.displayName.equalsIgnoreCase(name) || type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return DEPOSIT; // Default fallback
    }
    
    /**
     * Check if this transaction type represents income.
     * 
     * @return true if this is an income type
     */
    public boolean isIncome() {
        return this == DEPOSIT || this == SALARY_PAYMENT || this == BONUS || this == REFUND;
    }
    
    /**
     * Check if this transaction type represents an expense.
     * 
     * @return true if this is an expense type
     */
    public boolean isExpense() {
        return this == WITHDRAWAL || this == TAX_COLLECTION || this == TERRITORY_COST || 
               this == ALLIANCE_COST || this == WAR_COST || this == MAINTENANCE || this == FINE_PAYMENT;
    }
}