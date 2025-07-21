package xyz.inv1s1bl3.countries.core.economy;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.country.CitizenRole;
import xyz.inv1s1bl3.countries.core.country.Country;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the tax collection system for countries.
 * Handles tax rates, collection, and distribution.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
@Data
public final class TaxSystem {
    
    private final CountriesPlugin plugin;
    private final Map<String, LocalDateTime> lastTaxCollection;
    
    public TaxSystem(@NotNull final CountriesPlugin plugin) {
        this.plugin = plugin;
        this.lastTaxCollection = new HashMap<>();
    }
    
    /**
     * Collect taxes from all countries.
     * 
     * @return the total amount collected
     */
    public double collectAllTaxes() {
        double totalCollected = 0.0;
        
        for (final Country country : this.plugin.getCountryManager().getAllCountries()) {
            totalCollected += this.collectCountryTaxes(country);
        }
        
        return totalCollected;
    }
    
    /**
     * Collect taxes from a specific country.
     * 
     * @param country the country
     * @return the amount collected
     */
    public double collectCountryTaxes(@NotNull final Country country) {
        final LocalDateTime lastCollection = this.lastTaxCollection.get(country.getName());
        final LocalDateTime now = LocalDateTime.now();
        
        // Check if enough time has passed since last collection
        final int taxInterval = this.plugin.getConfigManager()
            .getConfigValue("economy.tax-collection-interval", 86400); // 24 hours in seconds
        
        if (lastCollection != null && lastCollection.plusSeconds(taxInterval).isAfter(now)) {
            return 0.0; // Too soon for next collection
        }
        
        double totalCollected = 0.0;
        int citizensTaxed = 0;
        
        // Collect from each citizen
        for (final Map.Entry<UUID, CitizenRole> entry : country.getCitizens().entrySet()) {
            final UUID citizenId = entry.getKey();
            final CitizenRole role = entry.getValue();
            
            final double taxAmount = this.calculateTaxAmount(citizenId, country, role);
            if (taxAmount > 0) {
                final BankAccount citizenAccount = this.plugin.getEconomyManager().getAccount(citizenId);
                if (citizenAccount != null && citizenAccount.getBalance() >= taxAmount) {
                    // Withdraw from citizen
                    if (citizenAccount.withdraw(taxAmount, "Tax payment to " + country.getName(), null)) {
                        // Deposit to country
                        country.deposit(taxAmount);
                        totalCollected += taxAmount;
                        citizensTaxed++;
                        
                        // Log transaction
                        this.plugin.getEconomyManager().logTransaction(new Transaction(
                            UUID.randomUUID(),
                            TransactionType.TAX_COLLECTION,
                            taxAmount,
                            "Tax collected from citizen",
                            citizenId,
                            this.getCountryAccountId(country)
                        ));
                    }
                }
            }
        }
        
        // Update last collection time
        this.lastTaxCollection.put(country.getName(), now);
        
        // Apply government efficiency
        final double efficiency = country.getGovernmentType().getTaxEfficiency();
        final double actualCollected = totalCollected * efficiency;
        
        // Adjust country balance if efficiency < 1.0
        if (efficiency < 1.0) {
            final double loss = totalCollected - actualCollected;
            country.withdraw(loss);
        }
        
        this.plugin.getLogger().info(String.format(
            "Collected $%.2f in taxes from %s (%d citizens, %.1f%% efficiency)",
            actualCollected, country.getName(), citizensTaxed, efficiency * 100));
        
        return actualCollected;
    }
    
    /**
     * Calculate tax amount for a citizen.
     * 
     * @param citizenId the citizen's UUID
     * @param country the country
     * @param role the citizen's role
     * @return the tax amount
     */
    private double calculateTaxAmount(@NotNull final UUID citizenId, 
                                    @NotNull final Country country, 
                                    @NotNull final CitizenRole role) {
        final BankAccount account = this.plugin.getEconomyManager().getAccount(citizenId);
        if (account == null) {
            return 0.0;
        }
        
        final double balance = account.getBalance();
        final double taxRate = country.getTaxRate();
        
        // Base tax calculation
        double taxAmount = balance * taxRate;
        
        // Apply role-based tax modifiers
        final double roleModifier = this.getRoleTaxModifier(role);
        taxAmount *= roleModifier;
        
        // Apply minimum and maximum tax limits
        final double minTax = this.plugin.getConfigManager()
            .getConfigValue("economy.min-tax-amount", 1.0);
        final double maxTax = this.plugin.getConfigManager()
            .getConfigValue("economy.max-tax-amount", 1000.0);
        
        taxAmount = Math.max(minTax, Math.min(maxTax, taxAmount));
        
        // Don't tax if balance is too low
        final double minTaxableBalance = this.plugin.getConfigManager()
            .getConfigValue("economy.min-taxable-balance", 100.0);
        
        if (balance < minTaxableBalance) {
            return 0.0;
        }
        
        return taxAmount;
    }
    
    /**
     * Get tax modifier based on citizen role.
     * 
     * @param role the citizen role
     * @return the tax modifier
     */
    private double getRoleTaxModifier(@NotNull final CitizenRole role) {
        switch (role) {
            case LEADER:
                return 0.5; // Leaders pay less tax
            case MINISTER:
                return 0.7;
            case OFFICER:
                return 0.9;
            case CITIZEN:
            default:
                return 1.0; // Citizens pay full tax
        }
    }
    
    /**
     * Distribute salaries to all countries.
     * 
     * @return the total amount distributed
     */
    public double distributeAllSalaries() {
        double totalDistributed = 0.0;
        
        for (final Country country : this.plugin.getCountryManager().getAllCountries()) {
            totalDistributed += this.distributeCountrySalaries(country);
        }
        
        return totalDistributed;
    }
    
    /**
     * Distribute salaries for a specific country.
     * 
     * @param country the country
     * @return the amount distributed
     */
    public double distributeCountrySalaries(@NotNull final Country country) {
        if (!this.plugin.getConfigManager().getConfigValue("economy.enable-salaries", true)) {
            return 0.0;
        }
        
        double totalDistributed = 0.0;
        int citizensPaid = 0;
        
        // Pay salary to each citizen based on their role
        for (final Map.Entry<UUID, CitizenRole> entry : country.getCitizens().entrySet()) {
            final UUID citizenId = entry.getKey();
            final CitizenRole role = entry.getValue();
            
            final double salary = country.getSalary(role);
            if (salary > 0 && country.getBalance() >= salary) {
                // Withdraw from country
                if (country.withdraw(salary)) {
                    // Deposit to citizen
                    final BankAccount citizenAccount = this.plugin.getEconomyManager().getAccount(citizenId);
                    if (citizenAccount != null) {
                        citizenAccount.deposit(salary, "Salary from " + country.getName(), 
                            this.getCountryAccountId(country));
                        totalDistributed += salary;
                        citizensPaid++;
                        
                        // Log transaction
                        this.plugin.getEconomyManager().logTransaction(new Transaction(
                            UUID.randomUUID(),
                            TransactionType.SALARY_PAYMENT,
                            salary,
                            "Salary payment to " + role.getDisplayName(),
                            this.getCountryAccountId(country),
                            citizenId
                        ));
                    }
                }
            }
        }
        
        if (citizensPaid > 0) {
            this.plugin.getLogger().info(String.format(
                "Distributed $%.2f in salaries from %s to %d citizens",
                totalDistributed, country.getName(), citizensPaid));
        }
        
        return totalDistributed;
    }
    
    /**
     * Calculate the total tax burden for a country.
     * 
     * @param country the country
     * @return the total tax burden
     */
    public double calculateTaxBurden(@NotNull final Country country) {
        double totalBurden = 0.0;
        
        for (final Map.Entry<UUID, CitizenRole> entry : country.getCitizens().entrySet()) {
            final UUID citizenId = entry.getKey();
            final CitizenRole role = entry.getValue();
            totalBurden += this.calculateTaxAmount(citizenId, country, role);
        }
        
        return totalBurden;
    }
    
    /**
     * Calculate the total salary cost for a country.
     * 
     * @param country the country
     * @return the total salary cost
     */
    public double calculateSalaryCost(@NotNull final Country country) {
        double totalCost = 0.0;
        
        for (final Map.Entry<UUID, CitizenRole> entry : country.getCitizens().entrySet()) {
            final CitizenRole role = entry.getValue();
            totalCost += country.getSalary(role);
        }
        
        return totalCost;
    }
    
    /**
     * Get the country account ID (using leader's UUID for now).
     * 
     * @param country the country
     * @return the account ID
     */
    private UUID getCountryAccountId(@NotNull final Country country) {
        // For now, use the leader's UUID as the country account ID
        // In a more advanced system, countries would have their own account IDs
        return country.getLeaderId();
    }
    
    /**
     * Check if a country can afford its salary obligations.
     * 
     * @param country the country
     * @return true if the country can afford salaries
     */
    public boolean canAffordSalaries(@NotNull final Country country) {
        final double salaryCost = this.calculateSalaryCost(country);
        return country.getBalance() >= salaryCost;
    }
    
    /**
     * Get tax collection statistics.
     * 
     * @return a map of statistics
     */
    @NotNull
    public Map<String, Object> getTaxStatistics() {
        final Map<String, Object> stats = new HashMap<>();
        
        double totalTaxBurden = 0.0;
        double totalSalaryCost = 0.0;
        int countriesWithTaxes = 0;
        
        for (final Country country : this.plugin.getCountryManager().getAllCountries()) {
            final double taxBurden = this.calculateTaxBurden(country);
            final double salaryCost = this.calculateSalaryCost(country);
            
            totalTaxBurden += taxBurden;
            totalSalaryCost += salaryCost;
            
            if (country.getTaxRate() > 0) {
                countriesWithTaxes++;
            }
        }
        
        stats.put("total_tax_burden", totalTaxBurden);
        stats.put("total_salary_cost", totalSalaryCost);
        stats.put("countries_with_taxes", countriesWithTaxes);
        stats.put("average_tax_rate", this.plugin.getCountryManager().getAllCountries().stream()
            .mapToDouble(Country::getTaxRate)
            .average()
            .orElse(0.0));
        
        return stats;
    }
}