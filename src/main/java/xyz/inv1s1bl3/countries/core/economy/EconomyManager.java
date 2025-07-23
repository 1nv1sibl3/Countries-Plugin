package xyz.inv1s1bl3.countries.core.economy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.country.Country;
import xyz.inv1s1bl3.countries.core.country.Citizen;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages the Countries economy system including accounts, transactions, and taxes.
 */
public class EconomyManager {
    
    private final CountriesPlugin plugin;
    private final Map<Integer, BankAccount> accounts;
    private final Map<UUID, Integer> playerAccounts; // Player UUID -> Account ID
    private final Map<String, Integer> countryAccounts; // Country name -> Account ID
    private final List<Transaction> recentTransactions;
    
    private BukkitRunnable taxCollectionTask;
    private BukkitRunnable salaryPaymentTask;
    
    public EconomyManager(CountriesPlugin plugin) {
        this.plugin = plugin;
        this.accounts = new ConcurrentHashMap<>();
        this.playerAccounts = new ConcurrentHashMap<>();
        this.countryAccounts = new ConcurrentHashMap<>();
        this.recentTransactions = new ArrayList<>();
    }
    
    /**
     * Initialize the economy system
     */
    public void initialize() {
        loadAccounts();
        startTaxCollection();
        startSalaryPayments();
        
        plugin.debug("Economy system initialized");
    }
    
    /**
     * Shutdown the economy system
     */
    public void shutdown() {
        if (taxCollectionTask != null) {
            taxCollectionTask.cancel();
        }
        if (salaryPaymentTask != null) {
            salaryPaymentTask.cancel();
        }
        
        plugin.debug("Economy system shutdown");
    }
    
    /**
     * Load all accounts from storage
     */
    private void loadAccounts() {
        // This will be implemented when storage system is complete
        plugin.debug("Economy accounts loading system ready");
    }
    
    /**
     * Get or create a player account
     */
    public BankAccount getPlayerAccount(UUID playerUUID) {
        Integer accountId = playerAccounts.get(playerUUID);
        if (accountId != null) {
            return accounts.get(accountId);
        }
        
        // Create new player account
        return createPlayerAccount(playerUUID);
    }
    
    /**
     * Get or create a country account
     */
    public BankAccount getCountryAccount(String countryName) {
        Integer accountId = countryAccounts.get(countryName.toLowerCase());
        if (accountId != null) {
            return accounts.get(accountId);
        }
        
        // Create new country account
        return createCountryAccount(countryName);
    }
    
    /**
     * Create a new player account
     */
    private BankAccount createPlayerAccount(UUID playerUUID) {
        int accountId = generateAccountId();
        double startingBalance = plugin.getConfigManager().getConfig()
                .getDouble("economy.starting-balance", 1000.0);
        
        BankAccount account = new BankAccount(accountId, AccountType.PLAYER, 
                playerUUID, null, startingBalance, System.currentTimeMillis());
        
        accounts.put(accountId, account);
        playerAccounts.put(playerUUID, accountId);
        
        // Save to storage
        saveAccount(account);
        
        plugin.debug("Created player account for: " + playerUUID);
        return account;
    }
    
    /**
     * Create a new country account
     */
    private BankAccount createCountryAccount(String countryName) {
        int accountId = generateAccountId();
        
        BankAccount account = new BankAccount(accountId, AccountType.COUNTRY, 
                null, countryName, 0.0, System.currentTimeMillis());
        
        accounts.put(accountId, account);
        countryAccounts.put(countryName.toLowerCase(), accountId);
        
        // Save to storage
        saveAccount(account);
        
        plugin.debug("Created country account for: " + countryName);
        return account;
    }
    
    /**
     * Transfer money between accounts
     */
    public boolean transferMoney(BankAccount fromAccount, BankAccount toAccount, 
                                double amount, TransactionType type, String description) {
        if (fromAccount == null || toAccount == null || amount <= 0) {
            return false;
        }
        
        if (!fromAccount.hasBalance(amount)) {
            return false;
        }
        
        try {
            // Perform transfer
            fromAccount.withdraw(amount);
            toAccount.deposit(amount);
            
            // Record transaction
            Transaction transaction = new Transaction(
                    generateTransactionId(), type, fromAccount.getAccountId(), 
                    toAccount.getAccountId(), amount, description, 
                    System.currentTimeMillis(), null
            );
            
            recordTransaction(transaction);
            
            // Save accounts
            saveAccount(fromAccount);
            saveAccount(toAccount);
            
            plugin.debug("Transfer completed: " + amount + " from " + 
                        fromAccount.getAccountId() + " to " + toAccount.getAccountId());
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error during money transfer", e);
            return false;
        }
    }
    
    /**
     * Deposit money to an account
     */
    public boolean depositMoney(BankAccount account, double amount, 
                               TransactionType type, String description) {
        if (account == null || amount <= 0) {
            return false;
        }
        
        try {
            account.deposit(amount);
            
            // Record transaction
            Transaction transaction = new Transaction(
                    generateTransactionId(), type, null, account.getAccountId(), 
                    amount, description, System.currentTimeMillis(), null
            );
            
            recordTransaction(transaction);
            saveAccount(account);
            
            plugin.debug("Deposit completed: " + amount + " to " + account.getAccountId());
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error during money deposit", e);
            return false;
        }
    }
    
    /**
     * Withdraw money from an account
     */
    public boolean withdrawMoney(BankAccount account, double amount, 
                                TransactionType type, String description) {
        if (account == null || amount <= 0 || !account.hasBalance(amount)) {
            return false;
        }
        
        try {
            account.withdraw(amount);
            
            // Record transaction
            Transaction transaction = new Transaction(
                    generateTransactionId(), type, account.getAccountId(), 
                    null, amount, description, System.currentTimeMillis(), null
            );
            
            recordTransaction(transaction);
            saveAccount(account);
            
            plugin.debug("Withdrawal completed: " + amount + " from " + account.getAccountId());
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error during money withdrawal", e);
            return false;
        }
    }
    
    /**
     * Start automatic tax collection
     */
    private void startTaxCollection() {
        long interval = plugin.getConfigManager().getConfig()
                .getLong("economy.tax-collection-interval", 86400) * 20L; // Convert to ticks
        
        if (interval <= 0) {
            plugin.debug("Tax collection is disabled");
            return;
        }
        
        taxCollectionTask = new BukkitRunnable() {
            @Override
            public void run() {
                collectTaxes();
            }
        };
        
        taxCollectionTask.runTaskTimerAsynchronously(plugin, interval, interval);
        plugin.debug("Tax collection started with interval: " + (interval / 20) + " seconds");
    }
    
    /**
     * Start automatic salary payments
     */
    private void startSalaryPayments() {
        // Run salary payments every hour
        long interval = 72000L; // 1 hour in ticks
        
        salaryPaymentTask = new BukkitRunnable() {
            @Override
            public void run() {
                paySalaries();
            }
        };
        
        salaryPaymentTask.runTaskTimerAsynchronously(plugin, interval, interval);
        plugin.debug("Salary payment system started");
    }
    
    /**
     * Collect taxes from all countries
     */
    private void collectTaxes() {
        plugin.debug("Starting tax collection...");
        
        for (Country country : plugin.getCountryManager().getAllCountries()) {
            collectCountryTaxes(country);
        }
        
        plugin.debug("Tax collection completed");
    }
    
    /**
     * Collect taxes from a specific country
     */
    private void collectCountryTaxes(Country country) {
        if (country.getTaxRate() <= 0) {
            return;
        }
        
        BankAccount countryAccount = getCountryAccount(country.getName());
        double totalTaxes = 0;
        int citizensTaxed = 0;
        
        for (Citizen citizen : country.getCitizens()) {
            BankAccount playerAccount = getPlayerAccount(citizen.getPlayerUUID());
            if (playerAccount == null) continue;
            
            double taxAmount = playerAccount.getBalance() * country.getTaxRate();
            if (taxAmount > 0 && playerAccount.hasBalance(taxAmount)) {
                if (transferMoney(playerAccount, countryAccount, taxAmount, 
                                TransactionType.TAX_COLLECTION, "Daily tax collection")) {
                    totalTaxes += taxAmount;
                    citizensTaxed++;
                    
                    // Notify player if online
                    Player player = Bukkit.getPlayer(citizen.getPlayerUUID());
                    if (player != null) {
                        ChatUtils.sendPrefixedMessage(player, 
                                "&6Tax collected: " + ChatUtils.formatCurrency(taxAmount));
                    }
                }
            }
        }
        
        if (totalTaxes > 0) {
            plugin.debug("Collected " + totalTaxes + " in taxes from " + 
                        country.getName() + " (" + citizensTaxed + " citizens)");
        }
    }
    
    /**
     * Pay salaries to all citizens
     */
    private void paySalaries() {
        plugin.debug("Starting salary payments...");
        
        for (Country country : plugin.getCountryManager().getAllCountries()) {
            payCountrySalaries(country);
        }
        
        plugin.debug("Salary payments completed");
    }
    
    /**
     * Pay salaries for a specific country
     */
    private void payCountrySalaries(Country country) {
        BankAccount countryAccount = getCountryAccount(country.getName());
        
        for (Citizen citizen : country.getCitizensDueForSalary()) {
            if (citizen.getSalary() <= 0) continue;
            
            BankAccount playerAccount = getPlayerAccount(citizen.getPlayerUUID());
            if (playerAccount == null) continue;
            
            if (countryAccount.hasBalance(citizen.getSalary())) {
                if (transferMoney(countryAccount, playerAccount, citizen.getSalary(), 
                                TransactionType.SALARY_PAYMENT, "Daily salary payment")) {
                    citizen.setLastSalaryPaid(System.currentTimeMillis());
                    
                    // Notify player if online
                    Player player = Bukkit.getPlayer(citizen.getPlayerUUID());
                    if (player != null) {
                        ChatUtils.sendPrefixedConfigMessage(player, "economy.salary-paid", 
                                ChatUtils.formatCurrency(citizen.getSalary()));
                    }
                }
            }
        }
    }
    
    /**
     * Record a transaction
     */
    private void recordTransaction(Transaction transaction) {
        recentTransactions.add(transaction);
        
        // Keep only recent transactions in memory (last 1000)
        if (recentTransactions.size() > 1000) {
            recentTransactions.remove(0);
        }
        
        // Save to storage
        saveTransaction(transaction);
    }
    
    /**
     * Get recent transactions for an account
     */
    public List<Transaction> getAccountTransactions(int accountId, int limit) {
        return recentTransactions.stream()
                .filter(t -> (t.getFromAccountId() != null && t.getFromAccountId() == accountId) ||
                           (t.getToAccountId() != null && t.getToAccountId() == accountId))
                .sorted((t1, t2) -> Long.compare(t2.getTransactionDate(), t1.getTransactionDate()))
                .limit(limit)
                .toList();
    }
    
    /**
     * Generate unique account ID
     */
    private int generateAccountId() {
        return accounts.size() + 1;
    }
    
    /**
     * Generate unique transaction ID
     */
    private int generateTransactionId() {
        return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
    }
    
    /**
     * Save account to storage
     */
    private void saveAccount(BankAccount account) {
        plugin.getDataManager().executeAsync(() -> {
            try {
                plugin.debug("Saving account: " + account.getAccountId());
                // This will be implemented when storage system is complete
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error saving account: " + account.getAccountId(), e);
            }
        });
    }
    
    /**
     * Save transaction to storage
     */
    private void saveTransaction(Transaction transaction) {
        plugin.getDataManager().executeAsync(() -> {
            try {
                plugin.debug("Saving transaction: " + transaction.getTransactionId());
                // This will be implemented when storage system is complete
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error saving transaction: " + transaction.getTransactionId(), e);
            }
        });
    }
    
    /**
     * Get economy statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        double totalPlayerBalance = accounts.values().stream()
                .filter(a -> a.getAccountType() == AccountType.PLAYER)
                .mapToDouble(BankAccount::getBalance)
                .sum();
        
        double totalCountryBalance = accounts.values().stream()
                .filter(a -> a.getAccountType() == AccountType.COUNTRY)
                .mapToDouble(BankAccount::getBalance)
                .sum();
        
        stats.put("total_accounts", accounts.size());
        stats.put("player_accounts", playerAccounts.size());
        stats.put("country_accounts", countryAccounts.size());
        stats.put("total_player_balance", totalPlayerBalance);
        stats.put("total_country_balance", totalCountryBalance);
        stats.put("total_transactions", recentTransactions.size());
        
        return stats;
    }
}