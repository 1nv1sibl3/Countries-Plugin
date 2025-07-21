package xyz.inv1s1bl3.countries.core.economy;

import com.google.gson.annotations.Expose;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a financial transaction in the economy system.
 * Records all money movements between accounts.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class Transaction {
    
    @Expose
    @EqualsAndHashCode.Include
    private final UUID transactionId;
    
    @Expose
    private final TransactionType type;
    
    @Expose
    private final double amount;
    
    @Expose
    private final String description;
    
    @Expose
    private final UUID fromAccount;
    
    @Expose
    private final UUID toAccount;
    
    @Expose
    private final LocalDateTime timestamp;
    
    @Expose
    private final String category;
    
    @Expose
    private boolean isReversed;
    
    /**
     * Create a new transaction.
     * 
     * @param transactionId the transaction ID
     * @param type the transaction type
     * @param amount the transaction amount
     * @param description the transaction description
     * @param fromAccount the source account (can be null)
     * @param toAccount the destination account (can be null)
     */
    public Transaction(@NotNull final UUID transactionId,
                      @NotNull final TransactionType type,
                      final double amount,
                      @NotNull final String description,
                      @Nullable final UUID fromAccount,
                      @Nullable final UUID toAccount) {
        this.transactionId = transactionId;
        this.type = type;
        this.amount = Math.abs(amount);
        this.description = description;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.timestamp = LocalDateTime.now();
        this.category = this.determineCategory(description);
        this.isReversed = false;
    }
    
    /**
     * Determine the transaction category based on description.
     * 
     * @param description the transaction description
     * @return the category
     */
    @NotNull
    private String determineCategory(@NotNull final String description) {
        final String lowerDesc = description.toLowerCase();
        
        if (lowerDesc.contains("tax")) {
            return "TAX";
        } else if (lowerDesc.contains("salary")) {
            return "SALARY";
        } else if (lowerDesc.contains("trade") || lowerDesc.contains("market")) {
            return "TRADE";
        } else if (lowerDesc.contains("territory") || lowerDesc.contains("claim")) {
            return "TERRITORY";
        } else if (lowerDesc.contains("fine") || lowerDesc.contains("penalty")) {
            return "PENALTY";
        } else if (lowerDesc.contains("transfer")) {
            return "TRANSFER";
        } else {
            return "OTHER";
        }
    }
    
    /**
     * Check if this transaction involves a specific account.
     * 
     * @param accountId the account ID
     * @return true if the account is involved
     */
    public boolean involvesAccount(@NotNull final UUID accountId) {
        return accountId.equals(this.fromAccount) || accountId.equals(this.toAccount);
    }
    
    /**
     * Get the other account involved in this transaction.
     * 
     * @param accountId the known account ID
     * @return the other account ID, or null if not found
     */
    @Nullable
    public UUID getOtherAccount(@NotNull final UUID accountId) {
        if (accountId.equals(this.fromAccount)) {
            return this.toAccount;
        } else if (accountId.equals(this.toAccount)) {
            return this.fromAccount;
        }
        return null;
    }
    
    /**
     * Check if this is an income transaction for the specified account.
     * 
     * @param accountId the account ID
     * @return true if this is income for the account
     */
    public boolean isIncomeFor(@NotNull final UUID accountId) {
        return accountId.equals(this.toAccount) && this.type == TransactionType.DEPOSIT;
    }
    
    /**
     * Check if this is an expense transaction for the specified account.
     * 
     * @param accountId the account ID
     * @return true if this is an expense for the account
     */
    public boolean isExpenseFor(@NotNull final UUID accountId) {
        return accountId.equals(this.fromAccount) && this.type == TransactionType.WITHDRAWAL;
    }
    
    /**
     * Reverse this transaction (mark as reversed).
     */
    public void reverse() {
        this.isReversed = true;
    }
    
    /**
     * Get a formatted string representation of the transaction.
     * 
     * @return formatted transaction string
     */
    @NotNull
    public String getFormattedString() {
        return String.format("[%s] %s: $%.2f - %s", 
            this.timestamp.toString().substring(0, 19),
            this.type.getDisplayName(),
            this.amount,
            this.description);
    }
}