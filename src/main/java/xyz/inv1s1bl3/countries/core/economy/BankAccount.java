package xyz.inv1s1bl3.countries.core.economy;

import java.util.UUID;

/**
 * Represents a bank account in the Countries economy system.
 */
public class BankAccount {
    
    private final int accountId;
    private final AccountType accountType;
    private final UUID ownerUUID; // null for country/system accounts
    private final String countryName; // null for player/system accounts
    private double balance;
    private final long createdDate;
    private long lastTransactionDate;
    private boolean frozen;
    
    public BankAccount(int accountId, AccountType accountType, UUID ownerUUID, 
                      String countryName, double balance, long createdDate) {
        this.accountId = accountId;
        this.accountType = accountType;
        this.ownerUUID = ownerUUID;
        this.countryName = countryName;
        this.balance = Math.max(0, balance);
        this.createdDate = createdDate;
        this.lastTransactionDate = createdDate;
        this.frozen = false;
    }
    
    // Getters
    public int getAccountId() {
        return accountId;
    }
    
    public AccountType getAccountType() {
        return accountType;
    }
    
    public UUID getOwnerUUID() {
        return ownerUUID;
    }
    
    public String getCountryName() {
        return countryName;
    }
    
    public double getBalance() {
        return balance;
    }
    
    public long getCreatedDate() {
        return createdDate;
    }
    
    public long getLastTransactionDate() {
        return lastTransactionDate;
    }
    
    public boolean isFrozen() {
        return frozen;
    }
    
    // Setters
    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }
    
    /**
     * Check if account has sufficient balance
     */
    public boolean hasBalance(double amount) {
        return !frozen && balance >= amount;
    }
    
    /**
     * Withdraw money from account
     */
    public boolean withdraw(double amount) {
        if (!hasBalance(amount)) {
            return false;
        }
        
        balance -= amount;
        lastTransactionDate = System.currentTimeMillis();
        return true;
    }
    
    /**
     * Deposit money to account
     */
    public void deposit(double amount) {
        if (amount > 0 && !frozen) {
            balance += amount;
            lastTransactionDate = System.currentTimeMillis();
        }
    }
    
    /**
     * Set balance directly (for administrative purposes)
     */
    public void setBalance(double balance) {
        this.balance = Math.max(0, balance);
        lastTransactionDate = System.currentTimeMillis();
    }
    
    /**
     * Get account display name
     */
    public String getDisplayName() {
        switch (accountType) {
            case PLAYER -> {
                return ownerUUID != null ? "Player Account" : "Unknown Player";
            }
            case COUNTRY -> {
                return countryName != null ? countryName + " Treasury" : "Unknown Country";
            }
            case BANK -> {
                return "Central Bank";
            }
            case SYSTEM -> {
                return "System Account";
            }
            default -> {
                return "Unknown Account";
            }
        }
    }
    
    /**
     * Check if account is active (used recently)
     */
    public boolean isActive() {
        // Consider account active if used within last 30 days
        return (System.currentTimeMillis() - lastTransactionDate) < 2592000000L;
    }
    
    /**
     * Get days since account creation
     */
    public long getDaysSinceCreation() {
        return (System.currentTimeMillis() - createdDate) / 86400000;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BankAccount account = (BankAccount) obj;
        return accountId == account.accountId;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(accountId);
    }
    
    @Override
    public String toString() {
        return String.format("BankAccount{id=%d, type=%s, balance=%.2f}", 
                           accountId, accountType.getDisplayName(), balance);
    }
}