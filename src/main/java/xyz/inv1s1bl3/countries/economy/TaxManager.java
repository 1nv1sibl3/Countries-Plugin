package xyz.inv1s1bl3.countries.economy;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.database.entities.Country;
import xyz.inv1s1bl3.countries.database.entities.Player;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Manages tax collection and distribution
 */
@RequiredArgsConstructor
public final class TaxManager {
    
    private final CountriesPlugin plugin;
    private final VaultIntegration vaultIntegration;
    private final TransactionManager transactionManager;
    
    /**
     * Collect taxes from all countries
     */
    public void collectAllTaxes() {
        this.plugin.getLogger().info("Starting tax collection for all countries...");
        
        final List<Country> activeCountries = this.plugin.getCountryManager().getAllActiveCountries();
        int totalCollected = 0;
        
        for (final Country country : activeCountries) {
            try {
                final int collected = this.collectCountryTaxes(country);
                totalCollected += collected;
                
            } catch (final Exception exception) {
                this.plugin.getLogger().log(Level.SEVERE, 
                    "Error collecting taxes for country " + country.getName(), exception);
            }
        }
        
        this.plugin.getLogger().info("Tax collection completed. Collected from " + totalCollected + " citizens.");
    }
    
    /**
     * Collect taxes for a specific country
     * @param country Country to collect taxes from
     * @return Number of citizens taxed
     */
    public int collectCountryTaxes(final Country country) {
        if (country.getTaxRate() <= 0) {
            return 0; // No taxes to collect
        }
        
        final List<Player> members = this.plugin.getCountryManager().getCountryMembers(country.getId());
        int citizensTaxed = 0;
        double totalTaxCollected = 0.0;
        
        for (final Player member : members) {
            try {
                final double taxAmount = this.calculatePlayerTax(member, country.getTaxRate());
                
                if (taxAmount > 0 && this.collectTaxFromPlayer(member, country, taxAmount)) {
                    citizensTaxed++;
                    totalTaxCollected += taxAmount;
                }
                
            } catch (final Exception exception) {
                this.plugin.getLogger().log(Level.WARNING, 
                    "Error collecting tax from player " + member.getUsername(), exception);
            }
        }
        
        // Update country treasury
        if (totalTaxCollected > 0) {
            country.addToTreasury(totalTaxCollected);
            this.plugin.getCountryManager().updateCountry(country);
            
            // Record transaction
            this.transactionManager.recordPlayerToCountryTransaction(
                null, // System transaction
                country.getId(),
                totalTaxCollected,
                "Tax collection from " + citizensTaxed + " citizens",
                "tax",
                null
            );
            
            this.plugin.getLogger().info("Collected $" + String.format("%.2f", totalTaxCollected) + 
                " in taxes from " + citizensTaxed + " citizens of " + country.getName());
        }
        
        return citizensTaxed;
    }
    
    /**
     * Calculate tax amount for a player
     * @param player Player to calculate tax for
     * @param taxRate Tax rate percentage
     * @return Tax amount
     */
    public double calculatePlayerTax(final Player player, final double taxRate) {
        if (!this.vaultIntegration.isAvailable()) {
            return 0.0;
        }
        
        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUuid());
        final double balance = this.vaultIntegration.getBalance(offlinePlayer);
        
        // Calculate tax based on balance
        final double taxAmount = balance * (taxRate / 100.0);
        
        // Apply minimum and maximum tax limits
        final double minTax = this.plugin.getConfigManager().getMainConfig().getDouble("economy.min-tax", 0.0);
        final double maxTax = this.plugin.getConfigManager().getMainConfig().getDouble("economy.max-tax", 10000.0);
        
        return Math.max(minTax, Math.min(maxTax, taxAmount));
    }
    
    /**
     * Collect tax from a specific player
     * @param player Player to collect tax from
     * @param country Country collecting the tax
     * @param taxAmount Amount to collect
     * @return true if successful
     */
    public boolean collectTaxFromPlayer(final Player player, final Country country, final double taxAmount) {
        if (!this.vaultIntegration.isAvailable()) {
            return false;
        }
        
        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUuid());
        
        // Check if player has enough money
        if (!this.vaultIntegration.hasBalance(offlinePlayer, taxAmount)) {
            // Player doesn't have enough money - record as tax debt
            this.recordTaxDebt(player, country, taxAmount);
            return false;
        }
        
        // Withdraw tax from player
        if (!this.vaultIntegration.withdrawPlayer(offlinePlayer, taxAmount)) {
            return false;
        }
        
        // Record transaction
        this.transactionManager.recordPlayerToCountryTransaction(
            player.getUuid(),
            country.getId(),
            taxAmount,
            "Tax payment to " + country.getName(),
            "tax",
            null
        );
        
        // Notify player if online
        final org.bukkit.entity.Player onlinePlayer = Bukkit.getPlayer(player.getUuid());
        if (onlinePlayer != null) {
            xyz.inv1s1bl3.countries.utils.MessageUtil.sendMessage(onlinePlayer, "economy.tax-paid", 
                java.util.Map.of(
                    "amount", this.vaultIntegration.formatMoney(taxAmount),
                    "country", country.getName()
                ));
        }
        
        return true;
    }
    
    /**
     * Record tax debt for a player
     * @param player Player with tax debt
     * @param country Country owed taxes
     * @param amount Amount owed
     */
    private void recordTaxDebt(final Player player, final Country country, final double amount) {
        // TODO: Implement tax debt system when legal system is ready
        this.plugin.getLogger().warning("Player " + player.getUsername() + 
            " owes $" + String.format("%.2f", amount) + " in taxes to " + country.getName());
    }
    
    /**
     * Pay salaries to all country members
     */
    public void payAllSalaries() {
        this.plugin.getLogger().info("Starting salary payments for all countries...");
        
        final List<Country> activeCountries = this.plugin.getCountryManager().getAllActiveCountries();
        int totalPaid = 0;
        
        for (final Country country : activeCountries) {
            try {
                final int paid = this.payCountrySalaries(country);
                totalPaid += paid;
                
            } catch (final Exception exception) {
                this.plugin.getLogger().log(Level.SEVERE, 
                    "Error paying salaries for country " + country.getName(), exception);
            }
        }
        
        this.plugin.getLogger().info("Salary payments completed. Paid " + totalPaid + " salaries.");
    }
    
    /**
     * Pay salaries for a specific country
     * @param country Country to pay salaries for
     * @return Number of salaries paid
     */
    public int payCountrySalaries(final Country country) {
        final List<Player> members = this.plugin.getCountryManager().getCountryMembers(country.getId());
        int salariesPaid = 0;
        double totalSalariesPaid = 0.0;
        
        for (final Player member : members) {
            try {
                final double salary = this.calculatePlayerSalary(member, country);
                
                if (salary > 0 && this.paySalaryToPlayer(member, country, salary)) {
                    salariesPaid++;
                    totalSalariesPaid += salary;
                }
                
            } catch (final Exception exception) {
                this.plugin.getLogger().log(Level.WARNING, 
                    "Error paying salary to player " + member.getUsername(), exception);
            }
        }
        
        // Deduct from country treasury
        if (totalSalariesPaid > 0) {
            if (country.hasSufficientFunds(totalSalariesPaid)) {
                country.removeFromTreasury(totalSalariesPaid);
                this.plugin.getCountryManager().updateCountry(country);
                
                this.plugin.getLogger().info("Paid $" + String.format("%.2f", totalSalariesPaid) + 
                    " in salaries to " + salariesPaid + " members of " + country.getName());
            } else {
                this.plugin.getLogger().warning("Country " + country.getName() + 
                    " has insufficient funds to pay salaries (need $" + 
                    String.format("%.2f", totalSalariesPaid) + ")");
            }
        }
        
        return salariesPaid;
    }
    
    /**
     * Calculate salary for a player based on their role
     * @param player Player to calculate salary for
     * @param country Country the player belongs to
     * @return Salary amount
     */
    public double calculatePlayerSalary(final Player player, final Country country) {
        // Get base salary from government configuration
        final String governmentType = country.getGovernmentType();
        final String role = player.getRole();
        
        // Base salary from configuration
        double baseSalary = this.plugin.getConfigManager().getGovernmentConfig()
            .getDouble("government-types." + governmentType + ".roles." + role + ".salary", 0.0);
        
        // Apply country-specific multipliers
        final double salaryMultiplier = this.plugin.getConfigManager().getMainConfig()
            .getDouble("economy.salary-multiplier", 1.0);
        
        return baseSalary * salaryMultiplier;
    }
    
    /**
     * Pay salary to a specific player
     * @param player Player to pay salary to
     * @param country Country paying the salary
     * @param salaryAmount Amount to pay
     * @return true if successful
     */
    public boolean paySalaryToPlayer(final Player player, final Country country, final double salaryAmount) {
        if (!this.vaultIntegration.isAvailable()) {
            return false;
        }
        
        // Check if country has enough money
        if (!country.hasSufficientFunds(salaryAmount)) {
            return false;
        }
        
        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUuid());
        
        // Deposit salary to player
        if (!this.vaultIntegration.depositPlayer(offlinePlayer, salaryAmount)) {
            return false;
        }
        
        // Record transaction
        this.transactionManager.recordCountryToPlayerTransaction(
            country.getId(),
            player.getUuid(),
            salaryAmount,
            "Salary payment for role: " + player.getRole(),
            "salary",
            null
        );
        
        // Notify player if online
        final org.bukkit.entity.Player onlinePlayer = Bukkit.getPlayer(player.getUuid());
        if (onlinePlayer != null) {
            xyz.inv1s1bl3.countries.utils.MessageUtil.sendMessage(onlinePlayer, "economy.salary-paid", 
                java.util.Map.of(
                    "amount", this.vaultIntegration.formatMoney(salaryAmount),
                    "country", country.getName()
                ));
        }
        
        return true;
    }
    
    /**
     * Set tax rate for a country
     * @param country Country to set tax rate for
     * @param newTaxRate New tax rate percentage
     * @return true if successful
     */
    public boolean setCountryTaxRate(final Country country, final double newTaxRate) {
        final double maxTaxRate = this.plugin.getConfigManager().getMainConfig()
            .getDouble("economy.max-tax-rate", 20.0);
        
        if (newTaxRate < 0 || newTaxRate > maxTaxRate) {
            return false;
        }
        
        country.setTaxRate(newTaxRate);
        this.plugin.getCountryManager().updateCountry(country);
        
        return true;
    }
}