package xyz.inv1s1bl3.countries.core.economy;

import com.google.gson.annotations.Expose;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a bank account for players or countries.
 * Handles balance management and transaction history.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class BankAccount {
    
    @Expose
    @EqualsAndHashCode.Include
    private final UUID accountId;
    
    @Expose
    private final String accountType; // "PLAYER" or "COUNTRY"
    
    @Expose
    private final String ownerName;
    
    @Expose
    private double balance;
    
    @Expose
    private final LocalDateTime createdDate;
    
    @Expose
    private LocalDateTime lastActivity;
    
    @Expose
    private final List<Transaction> transactionHistory;
    
    @Expose
    private boolean isActive;
    
    @Expose
    private boolean isFrozen;
    
    /**
     * Create a new bank account.
     * 
     * @param accountId the account ID (player UUID or country UUID)
     * @param accountType the account type
     * @param ownerName the owner name
     * @param initialBalance the initial balance
     */
    public BankAccount(@NotNull final UUID accountId,
                      @NotNull final String accountType,
                      @NotNull final String ownerName,
                      final double initialBalance) {
        this.accountId = accountId;
        this.accountType = accountType;
        this.ownerName = ownerName;
        this.balance = Math.max(0.0, initialBalance);
        this.createdDate = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
        this.transactionHistory = new ArrayList<>();
        this.isActive = true;
        this.isFrozen = false;
        
        // Add initial deposit transaction if balance > 0
        if (initialBalance > 0) {
            this.addTransaction(new Transaction(
                UUID.randomUUID(),
                TransactionType.DEPOSIT,
                initialBalance,
                "Initial deposit",
                null,
                this.accountId
            ));
        }
    }
    
    /**
     * Deposit money into the account.
     * 
     * @param amount the amount to deposit
     * @param description the transaction description
     * @param fromAccount the source account (can be null)
     * @return true if successful
     */
    public boolean deposit(final double amount, @NotNull final String description, final UUID fromAccount) {
        if (amount <= 0 || this.isFrozen || !this.isActive) {
            return false;
        }
        
        this.balance += amount;
        this.updateActivity();
        
        this.addTransaction(new Transaction(
            UUID.randomUUID(),
            TransactionType.DEPOSIT,
            amount,
            description,
            fromAccount,
            this.accountId
        ));
        
        return true;
    }
    
    /**
     * Withdraw money from the account.
     * 
     * @param amount the amount to withdraw
     * @param description the transaction description
     * @param toAccount the destination account (can be null)
     * @return true if successful
     */
    public boolean withdraw(final double amount, @NotNull final String description, final UUID toAccount) {
        if (amount <= 0 || this.balance < amount || this.isFrozen || !this.isActive) {
            return false;
        }
        
        this.balance -= amount;
        this.updateActivity();
        
        this.addTransaction(new Transaction(
            UUID.randomUUID(),
            TransactionType.WITHDRAWAL,
            amount,
            description,
            this.accountId,
            toAccount
        ));
        
        return true;
    }
    
    /**
     * Transfer money to another account.
     * 
     * @param amount the amount to transfer
     * @param description the transaction description
     * @param toAccount the destination account ID
     * @return true if successful
     */
    public boolean transfer(final double amount, @NotNull final String description, @NotNull final UUID toAccount) {
        return this.withdraw(amount, description, toAccount);
    }
    
    /**
     * Add a transaction to the history.
     * 
     * @param transaction the transaction to add
     */
    private void addTransaction(@NotNull final Transaction transaction) {
        this.transactionHistory.add(transaction);
        
        // Keep only the last 100 transactions to prevent memory issues
        if (this.transactionHistory.size() > 100) {
            this.transactionHistory.remove(0);
        }
    }
    
    /**
     * Get recent transactions.
     * 
     * @param limit the maximum number of transactions to return
     * @return a list of recent transactions
     */
    @NotNull
    public List<Transaction> getRecentTransactions(final int limit) {
        final int size = this.transactionHistory.size();
        final int fromIndex = Math.max(0, size - limit);
        return new ArrayList<>(this.transactionHistory.subList(fromIndex, size));
    }
    
    /**
     * Get transactions by type.
     * 
     * @param type the transaction type
     * @param limit the maximum number of transactions to return
     * @return a list of transactions
     */
    @NotNull
    public List<Transaction> getTransactionsByType(@NotNull final TransactionType type, final int limit) {
        return this.transactionHistory.stream()
            .filter(transaction -> transaction.getType() == type)
            .skip(Math.max(0, this.transactionHistory.size() - limit))
            .toList();
    }
    
    /**
     * Calculate total income from transactions.
     * 
     * @return the total income
     */
    public double getTotalIncome() {
        return this.transactionHistory.stream()
            .filter(transaction -> transaction.getType() == TransactionType.DEPOSIT)
            .mapToDouble(Transaction::getAmount)
            .sum();
    }
    
    /**
     * Calculate total expenses from transactions.
     * 
     * @return the total expenses
     */
    public double getTotalExpenses() {
        return this.transactionHistory.stream()
            .filter(transaction -> transaction.getType() == TransactionType.WITHDRAWAL)
            .mapToDouble(Transaction::getAmount)
            .sum();
    }
    
    /**
     * Update the last activity timestamp.
     */
    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }
    
    /**
     * Check if the account has been inactive for a certain number of days.
     * 
     * @param days the number of days
     * @return true if inactive for the specified days
     */
    public boolean isInactiveFor(final int days) {
        return this.lastActivity.isBefore(LocalDateTime.now().minusDays(days));
    }
    
    /**
     * Freeze the account to prevent transactions.
     */
    public void freeze() {
        this.isFrozen = true;
        this.updateActivity();
    }
    
    /**
     * Unfreeze the account to allow transactions.
     */
    public void unfreeze() {
        this.isFrozen = false;
        this.updateActivity();
    }
    
    /**
     * Close the account.
     */
    public void close() {
        this.isActive = false;
        this.updateActivity();
    }
    
    /**
     * Reopen the account.
     */
    public void reopen() {
        this.isActive = true;
        this.updateActivity();
    }
}