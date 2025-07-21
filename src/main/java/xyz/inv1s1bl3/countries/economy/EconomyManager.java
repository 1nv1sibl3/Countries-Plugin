package xyz.inv1s1bl3.countries.economy;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.database.repositories.TransactionRepository;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Manager for economy-related operations
 */
@Getter
public final class EconomyManager {
    
    private final CountriesPlugin plugin;
    private final TransactionRepository transactionRepository;
    private final VaultIntegration vaultIntegration;
    private final TransactionManager transactionManager;
    private final TaxManager taxManager;
    
    // Scheduled tasks
    private BukkitTask taxCollectionTask;
    private BukkitTask salaryPaymentTask;
    
    public EconomyManager(final CountriesPlugin plugin) {
        this.plugin = plugin;
        this.transactionRepository = new TransactionRepository(plugin);
        this.vaultIntegration = new VaultIntegration(plugin, plugin.getVaultEconomy());
        this.transactionManager = new TransactionManager(plugin, this.transactionRepository);
        this.taxManager = new TaxManager(plugin, this.vaultIntegration, this.transactionManager);
    }
    
    /**
     * Initialize the economy manager
     */
    public void initialize() {
        this.plugin.getLogger().info("Initializing Economy Manager...");
        
        // Start scheduled tasks if enabled
        if (this.plugin.getConfigManager().getMainConfig().getBoolean("economy.enable-taxes", true)) {
            this.startTaxCollection();
        }
        
        if (this.plugin.getConfigManager().getMainConfig().getBoolean("economy.enable-salaries", true)) {
            this.startSalaryPayments();
        }
        
        this.plugin.getLogger().info("Economy Manager initialized successfully!");
    }
    
    /**
     * Start automatic tax collection
     */
    private void startTaxCollection() {
        final int taxInterval = this.plugin.getConfigManager().getMainConfig()
            .getInt("economy.tax-collection-interval", 24) * 20 * 60 * 60; // Convert hours to ticks
        
        this.taxCollectionTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
            this.plugin,
            () -> {
                try {
                    this.taxManager.collectAllTaxes();
                } catch (final Exception exception) {
                    this.plugin.getLogger().log(Level.SEVERE, "Error during automatic tax collection", exception);
                }
            },
            taxInterval, // Initial delay
            taxInterval  // Period
        );
        
        this.plugin.getLogger().info("Automatic tax collection started (interval: " + 
            (taxInterval / 20 / 60 / 60) + " hours)");
    }
    
    /**
     * Start automatic salary payments
     */
    private void startSalaryPayments() {
        final int salaryInterval = this.plugin.getConfigManager().getMainConfig()
            .getInt("economy.salary-interval", 24) * 20 * 60 * 60; // Convert hours to ticks
        
        this.salaryPaymentTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
            this.plugin,
            () -> {
                try {
                    this.taxManager.payAllSalaries();
                } catch (final Exception exception) {
                    this.plugin.getLogger().log(Level.SEVERE, "Error during automatic salary payments", exception);
                }
            },
            salaryInterval, // Initial delay
            salaryInterval  // Period
        );
        
        this.plugin.getLogger().info("Automatic salary payments started (interval: " + 
            (salaryInterval / 20 / 60 / 60) + " hours)");
    }
    
    /**
     * Transfer money between players
     * @param fromUuid Sender UUID
     * @param toUuid Receiver UUID
     * @param amount Amount to transfer
     * @param description Transfer description
     * @return true if successful
     */
    public boolean transferPlayerToPlayer(final UUID fromUuid, final UUID toUuid, final double amount, final String description) {
        if (!this.vaultIntegration.isAvailable()) {
            return false;
        }
        
        final OfflinePlayer fromPlayer = Bukkit.getOfflinePlayer(fromUuid);
        final OfflinePlayer toPlayer = Bukkit.getOfflinePlayer(toUuid);
        
        if (this.vaultIntegration.transferMoney(fromPlayer, toPlayer, amount)) {
            // Record transaction
            this.transactionManager.recordPlayerToPlayerTransaction(
                fromUuid, toUuid, amount, description, "transfer", null
            );
            return true;
        }
        
        return false;
    }
    
    /**
     * Transfer money from player to country
     * @param fromUuid Player UUID
     * @param toCountryId Country ID
     * @param amount Amount to transfer
     * @param description Transfer description
     * @return true if successful
     */
    public boolean transferPlayerToCountry(final UUID fromUuid, final Integer toCountryId, final double amount, final String description) {
        if (!this.vaultIntegration.isAvailable()) {
            return false;
        }
        
        final OfflinePlayer fromPlayer = Bukkit.getOfflinePlayer(fromUuid);
        
        // Check if player has enough money
        if (!this.vaultIntegration.hasBalance(fromPlayer, amount)) {
            return false;
        }
        
        // Withdraw from player
        if (!this.vaultIntegration.withdrawPlayer(fromPlayer, amount)) {
            return false;
        }
        
        // Add to country treasury
        final var countryOpt = this.plugin.getCountryManager().getCountry(toCountryId);
        if (countryOpt.isPresent()) {
            final var country = countryOpt.get();
            country.addToTreasury(amount);
            this.plugin.getCountryManager().updateCountry(country);
            
            // Record transaction
            this.transactionManager.recordPlayerToCountryTransaction(
                fromUuid, toCountryId, amount, description, "transfer", null
            );
            
            return true;
        }
        
        // Refund player if country not found
        this.vaultIntegration.depositPlayer(fromPlayer, amount);
        return false;
    }
    
    /**
     * Transfer money from country to player
     * @param fromCountryId Country ID
     * @param toUuid Player UUID
     * @param amount Amount to transfer
     * @param description Transfer description
     * @return true if successful
     */
    public boolean transferCountryToPlayer(final Integer fromCountryId, final UUID toUuid, final double amount, final String description) {
        if (!this.vaultIntegration.isAvailable()) {
            return false;
        }
        
        final var countryOpt = this.plugin.getCountryManager().getCountry(fromCountryId);
        if (countryOpt.isEmpty()) {
            return false;
        }
        
        final var country = countryOpt.get();
        
        // Check if country has enough money
        if (!country.hasSufficientFunds(amount)) {
            return false;
        }
        
        final OfflinePlayer toPlayer = Bukkit.getOfflinePlayer(toUuid);
        
        // Deposit to player
        if (!this.vaultIntegration.depositPlayer(toPlayer, amount)) {
            return false;
        }
        
        // Remove from country treasury
        country.removeFromTreasury(amount);
        this.plugin.getCountryManager().updateCountry(country);
        
        // Record transaction
        this.transactionManager.recordCountryToPlayerTransaction(
            fromCountryId, toUuid, amount, description, "transfer", null
        );
        
        return true;
    }
    
    /**
     * Get player balance
     * @param playerUuid Player UUID
     * @return Player balance
     */
    public double getPlayerBalance(final UUID playerUuid) {
        if (!this.vaultIntegration.isAvailable()) {
            return 0.0;
        }
        
        return this.vaultIntegration.getBalance(playerUuid);
    }
    
    /**
     * Get country treasury balance
     * @param countryId Country ID
     * @return Treasury balance
     */
    public double getCountryBalance(final Integer countryId) {
        final var countryOpt = this.plugin.getCountryManager().getCountry(countryId);
        return countryOpt.map(xyz.inv1s1bl3.countries.database.entities.Country::getTreasuryBalance).orElse(0.0);
    }
    
    /**
     * Format money amount
     * @param amount Amount to format
     * @return Formatted money string
     */
    public String formatMoney(final double amount) {
        return this.vaultIntegration.formatMoney(amount);
    }
    
    /**
     * Save all economy data
     */
    public void saveAllData() {
        this.plugin.getLogger().info("Saving all economy data...");
        
        // Economy data is saved automatically through database operations
        // No additional saving needed
        
        this.plugin.getLogger().info("All economy data saved successfully!");
    }
    
    /**
     * Reload economy manager
     */
    public void reload() {
        this.plugin.getLogger().info("Reloading Economy Manager...");
        
        // Stop existing tasks
        if (this.taxCollectionTask != null) {
            this.taxCollectionTask.cancel();
        }
        
        if (this.salaryPaymentTask != null) {
            this.salaryPaymentTask.cancel();
        }
        
        // Restart tasks with new configuration
        if (this.plugin.getConfigManager().getMainConfig().getBoolean("economy.enable-taxes", true)) {
            this.startTaxCollection();
        }
        
        if (this.plugin.getConfigManager().getMainConfig().getBoolean("economy.enable-salaries", true)) {
            this.startSalaryPayments();
        }
        
        this.plugin.getLogger().info("Economy Manager reloaded successfully!");
    }
    
    /**
     * Shutdown economy manager
     */
    public void shutdown() {
        this.plugin.getLogger().info("Shutting down Economy Manager...");
        
        // Cancel scheduled tasks
        if (this.taxCollectionTask != null) {
            this.taxCollectionTask.cancel();
        }
        
        if (this.salaryPaymentTask != null) {
            this.salaryPaymentTask.cancel();
        }
        
        this.plugin.getLogger().info("Economy Manager shut down successfully!");
    }
}