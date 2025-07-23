package xyz.inv1s1bl3.countries.core.economy;

import xyz.inv1s1bl3.countries.utils.ChatUtils;

/**
 * Represents a financial transaction in the Countries economy system.
 */
public class Transaction {
    
    private final int transactionId;
    private final TransactionType type;
    private final Integer fromAccountId; // null for deposits
    private final Integer toAccountId; // null for withdrawals
    private final double amount;
    private final String description;
    private final long transactionDate;
    private final String additionalData; // JSON for extra data
    
    public Transaction(int transactionId, TransactionType type, Integer fromAccountId, 
                      Integer toAccountId, double amount, String description, 
                      long transactionDate, String additionalData) {
        this.transactionId = transactionId;
        this.type = type;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.description = description != null ? description : "";
        this.transactionDate = transactionDate;
        this.additionalData = additionalData;
    }
    
    // Getters
    public int getTransactionId() {
        return transactionId;
    }
    
    public TransactionType getType() {
        return type;
    }
    
    public Integer getFromAccountId() {
        return fromAccountId;
    }
    
    public Integer getToAccountId() {
        return toAccountId;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public long getTransactionDate() {
        return transactionDate;
    }
    
    public String getAdditionalData() {
        return additionalData;
    }
    
    /**
     * Check if this is a deposit transaction
     */
    public boolean isDeposit() {
        return fromAccountId == null && toAccountId != null;
    }
    
    /**
     * Check if this is a withdrawal transaction
     */
    public boolean isWithdrawal() {
        return fromAccountId != null && toAccountId == null;
    }
    
    /**
     * Check if this is a transfer transaction
     */
    public boolean isTransfer() {
        return fromAccountId != null && toAccountId != null;
    }
    
    /**
     * Get formatted transaction description
     */
    public String getFormattedDescription() {
        StringBuilder builder = new StringBuilder();
        
        builder.append(ChatUtils.colorize("&6")).append(type.getDisplayName());
        builder.append(ChatUtils.colorize(" &7- "));
        builder.append(ChatUtils.formatCurrency(amount));
        
        if (!description.isEmpty()) {
            builder.append(ChatUtils.colorize(" &8(")).append(description).append(")");
        }
        
        return builder.toString();
    }
    
    /**
     * Get days since transaction
     */
    public long getDaysSinceTransaction() {
        return (System.currentTimeMillis() - transactionDate) / 86400000;
    }
    
    /**
     * Check if transaction is recent (within 24 hours)
     */
    public boolean isRecent() {
        return getDaysSinceTransaction() < 1;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Transaction transaction = (Transaction) obj;
        return transactionId == transaction.transactionId;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(transactionId);
    }
    
    @Override
    public String toString() {
        return String.format("Transaction{id=%d, type=%s, amount=%.2f, from=%s, to=%s}", 
                           transactionId, type.name(), amount, fromAccountId, toAccountId);
    }
}