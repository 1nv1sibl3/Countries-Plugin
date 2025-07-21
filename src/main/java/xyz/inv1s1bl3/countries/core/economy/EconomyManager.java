package xyz.inv1s1bl3.countries.core.economy;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.inv1s1bl3.countries.CountriesPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the economy system for the plugin.
 * Handles bank accounts, transactions, taxes, and salaries.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
public final class EconomyManager {
    
    private final CountriesPlugin plugin;
    
    @Getter
    private final Map<UUID, BankAccount> accounts;
    private final List<Transaction> globalTransactionLog;
    private final TaxSystem taxSystem;
    
    public EconomyManager(@NotNull final CountriesPlugin plugin) {
        this.plugin = plugin;
        this.accounts = new ConcurrentHashMap<>();
        this.globalTransactionLog = new ArrayList<>();
        this.taxSystem = new TaxSystem(plugin);
    }
    
    /**
     * Create a new bank account.
     * 
     * @param accountId the account ID
     * @param accountType the account type ("PLAYER" or "COUNTRY")
     * @param ownerName the owner name
     * @param initialBalance the initial balance
     * @return the created account
     */
    @NotNull
    public BankAccount createAccount(@NotNull final UUID accountId,
                                   @NotNull final String accountType,
                                   @NotNull final String ownerName,
                                   final double initialBalance) {
        final BankAccount account = new BankAccount(accountId, accountType, ownerName, initialBalance);
        this.accounts.put(accountId, account);
        
        this.plugin.getDataManager().saveEconomyData();
        return account;
    }
    
    /**
     * Get or create a bank account for a player.
     * 
     * @param playerId the player's UUID
     * @return the bank account
     */
    @NotNull
    public BankAccount getOrCreatePlayerAccount(@NotNull final UUID playerId) {
        BankAccount account = this.accounts.get(playerId);
        if (account == null) {
            final OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
            final String playerName = player.getName() != null ? player.getName() : "Unknown Player";
            final double startingBalance = this.plugin.getConfigManager()
                .getConfigValue("economy.starting-balance", 1000.0);
            
            account = this.createAccount(playerId, "PLAYER", playerName, startingBalance);
        }
        return account;
    }
    
    /**
     * Get a bank account by ID.
     * 
     * @param accountId the account ID
     * @return the bank account, or null if not found
     */
    @Nullable
    public BankAccount getAccount(@NotNull final UUID accountId) {
        return this.accounts.get(accountId);
    }
    
    /**
     * Get a player's balance.
     * 
     * @param playerId the player's UUID
     * @return the balance
     */
    public double getBalance(@NotNull final UUID playerId) {
        final BankAccount account = this.getOrCreatePlayerAccount(playerId);
        return account.getBalance();
    }
    
    /**
     * Set a player's balance.
     * 
     * @param playerId the player's UUID
     * @param amount the new balance
     * @return true if successful
     */
    public boolean setBalance(@NotNull final UUID playerId, final double amount) {
        if (amount < 0) {
            return false;
        }
        
        final BankAccount account = this.getOrCreatePlayerAccount(playerId);
        final double currentBalance = account.getBalance();
        
        if (amount > currentBalance) {
            // Deposit the difference
            return account.deposit(amount - currentBalance, "Balance adjustment", null);
        } else if (amount < currentBalance) {
            // Withdraw the difference
            return account.withdraw(currentBalance - amount, "Balance adjustment", null);
        }
        
        return true; // Balance is already correct
    }
    
    /**
     * Deposit money into a player's account.
     * 
     * @param playerId the player's UUID
     * @param amount the amount to deposit
     * @param description the transaction description
     * @return true if successful
     */
    public boolean deposit(@NotNull final UUID playerId, final double amount, @NotNull final String description) {
        final BankAccount account = this.getOrCreatePlayerAccount(playerId);
        return account.deposit(amount, description, null);
    }
    
    /**
     * Withdraw money from a player's account.
     * 
     * @param playerId the player's UUID
     * @param amount the amount to withdraw
     * @return true if successful
     */
    public boolean withdraw(@NotNull final UUID playerId, final double amount) {
        return this.withdraw(playerId, amount, "Withdrawal");
    }
    
    /**
     * Withdraw money from a player's account with description.
     * 
     * @param playerId the player's UUID
     * @param amount the amount to withdraw
     * @param description the transaction description
     * @return true if successful
     */
    public boolean withdraw(@NotNull final UUID playerId, final double amount, @NotNull final String description) {
        final BankAccount account = this.getOrCreatePlayerAccount(playerId);
        return account.withdraw(amount, description, null);
    }
    
    /**
     * Transfer money between two players.
     * 
     * @param fromPlayerId the sender's UUID
     * @param toPlayerId the recipient's UUID
     * @param amount the amount to transfer
     * @param description the transaction description
     * @return true if successful
     */
    public boolean transfer(@NotNull final UUID fromPlayerId, 
                          @NotNull final UUID toPlayerId, 
                          final double amount, 
                          @NotNull final String description) {
        final BankAccount fromAccount = this.getOrCreatePlayerAccount(fromPlayerId);
        final BankAccount toAccount = this.getOrCreatePlayerAccount(toPlayerId);
        
        if (fromAccount.withdraw(amount, description, toPlayerId)) {
            if (toAccount.deposit(amount, description, fromPlayerId)) {
                // Log the transfer
                this.logTransaction(new Transaction(
                    UUID.randomUUID(),
                    TransactionType.TRANSFER,
                    amount,
                    description,
                    fromPlayerId,
                    toPlayerId
                ));
                return true;
            } else {
                // Refund if deposit failed
                fromAccount.deposit(amount, "Transfer refund", null);
            }
        }
        
        return false;
    }
    
    /**
     * Log a transaction to the global transaction log.
     * 
     * @param transaction the transaction to log
     */
    public void logTransaction(@NotNull final Transaction transaction) {
        this.globalTransactionLog.add(transaction);
        
        // Keep only the last 1000 transactions to prevent memory issues
        if (this.globalTransactionLog.size() > 1000) {
            this.globalTransactionLog.remove(0);
        }
        
        // Save data periodically
        if (this.globalTransactionLog.size() % 10 == 0) {
            this.plugin.getDataManager().saveEconomyData();
        }
    }
    
    /**
     * Get recent transactions for a player.
     * 
     * @param playerId the player's UUID
     * @param limit the maximum number of transactions
     * @return a list of transactions
     */
    @NotNull
    public List<Transaction> getPlayerTransactions(@NotNull final UUID playerId, final int limit) {
        final BankAccount account = this.getAccount(playerId);
        if (account == null) {
            return new ArrayList<>();
        }
        
        return account.getRecentTransactions(limit);
    }
    
    /**
     * Get global transaction statistics.
     * 
     * @return a map of statistics
     */
    @NotNull
    public Map<String, Object> getTransactionStatistics() {
        final Map<String, Object> stats = new HashMap<>();
        
        stats.put("total_transactions", this.globalTransactionLog.size());
        stats.put("total_accounts", this.accounts.size());
        
        final double totalBalance = this.accounts.values().stream()
            .mapToDouble(BankAccount::getBalance)
            .sum();
        stats.put("total_money_in_circulation", totalBalance);
        
        final Map<TransactionType, Long> transactionCounts = this.globalTransactionLog.stream()
            .collect(java.util.stream.Collectors.groupingBy(Transaction::getType, java.util.stream.Collectors.counting()));
        stats.put("transaction_type_distribution", transactionCounts);
        
        return stats;
    }
    
    /**
     * Get the richest players.
     * 
     * @param limit the number of players to return
     * @return a list of account IDs sorted by balance
     */
    @NotNull
    public List<UUID> getRichestPlayers(final int limit) {
        return this.accounts.entrySet().stream()
            .filter(entry -> "PLAYER".equals(entry.getValue().getAccountType()))
            .sorted((e1, e2) -> Double.compare(e2.getValue().getBalance(), e1.getValue().getBalance()))
            .limit(limit)
            .map(Map.Entry::getKey)
            .toList();
    }
    
    /**
     * Calculate the total wealth of all players.
     * 
     * @return the total wealth
     */
    public double getTotalPlayerWealth() {
        return this.accounts.values().stream()
            .filter(account -> "PLAYER".equals(account.getAccountType()))
            .mapToDouble(BankAccount::getBalance)
            .sum();
    }
    
    /**
     * Collect taxes from all countries.
     * 
     * @return the total amount collected
     */
    public double collectTaxes() {
        return this.taxSystem.collectAllTaxes();
    }
    
    /**
     * Distribute salaries to all countries.
     * 
     * @return the total amount distributed
     */
    public double distributeSalaries() {
        return this.taxSystem.distributeAllSalaries();
    }
    
    /**
     * Get the tax system.
     * 
     * @return the tax system
     */
    @NotNull
    public TaxSystem getTaxSystem() {
        return this.taxSystem;
    }
    
    /**
     * Load economy data from storage.
     * 
     * @param accountData the account data map
     * @param transactionData the transaction data list
     */
    public void loadEconomyData(@NotNull final Map<UUID, BankAccount> accountData,
                               @NotNull final List<Transaction> transactionData) {
        this.accounts.clear();
        this.globalTransactionLog.clear();
        
        this.accounts.putAll(accountData);
        this.globalTransactionLog.addAll(transactionData);
    }
    
    /**
     * Get all global transactions.
     * 
     * @return the global transaction log
     */
    @NotNull
    public List<Transaction> getGlobalTransactionLog() {
        return new ArrayList<>(this.globalTransactionLog);
    }
    
    /**
     * Clean up inactive accounts.
     * 
     * @return the number of accounts cleaned up
     */
    public int cleanupInactiveAccounts() {
        final int inactivityDays = this.plugin.getConfigManager()
            .getConfigValue("economy.account-inactivity-cleanup", 90);
        
        if (inactivityDays <= 0) {
            return 0;
        }
        
        int cleanedUp = 0;
        final List<UUID> toRemove = new ArrayList<>();
        
        for (final Map.Entry<UUID, BankAccount> entry : this.accounts.entrySet()) {
            final BankAccount account = entry.getValue();
            if (account.isInactiveFor(inactivityDays) && account.getBalance() == 0.0) {
                toRemove.add(entry.getKey());
                cleanedUp++;
            }
        }
        
        for (final UUID accountId : toRemove) {
            this.accounts.remove(accountId);
        }
        
        if (cleanedUp > 0) {
            this.plugin.getDataManager().saveEconomyData();
        }
        
        return cleanedUp;
    }
}