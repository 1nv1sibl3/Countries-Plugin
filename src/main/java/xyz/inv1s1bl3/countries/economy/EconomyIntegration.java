package xyz.inv1s1bl3.countries.economy;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.database.entities.Country;
import xyz.inv1s1bl3.countries.database.entities.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles integration between Countries plugin economy and external economy systems
 */
@RequiredArgsConstructor
public final class EconomyIntegration {
    
    private final CountriesPlugin plugin;
    private final VaultIntegration vaultIntegration;
    private final TransactionManager transactionManager;
    
    /**
     * Sync player balance with Vault
     * @param playerUuid Player UUID
     */
    public void syncPlayerBalance(final UUID playerUuid) {
        if (!this.vaultIntegration.isAvailable()) {
            return;
        }
        
        try {
            final Optional<Player> playerOpt = this.plugin.getCountryManager().getPlayer(playerUuid);
            if (playerOpt.isEmpty()) {
                return;
            }
            
            final Player player = playerOpt.get();
            final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
            
            // Get Vault balance
            final double vaultBalance = this.vaultIntegration.getBalance(offlinePlayer);
            
            // Update player balance in our system
            player.setBalance(vaultBalance);
            this.plugin.getCountryManager().updatePlayer(player);
            
        } catch (final Exception exception) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to sync player balance for " + playerUuid, exception);
        }
    }
    
    /**
     * Sync all online player balances
     */
    public void syncAllOnlinePlayerBalances() {
        if (!this.vaultIntegration.isAvailable()) {
            return;
        }
        
        Bukkit.getOnlinePlayers().forEach(player -> this.syncPlayerBalance(player.getUniqueId()));
    }
    
    /**
     * Handle player economy integration on join
     * @param playerUuid Player UUID
     */
    public void handlePlayerJoin(final UUID playerUuid) {
        // Sync balance
        this.syncPlayerBalance(playerUuid);
        
        // Check for pending transactions
        this.processPendingTransactions(playerUuid);
    }
    
    /**
     * Process any pending transactions for a player
     * @param playerUuid Player UUID
     */
    private void processPendingTransactions(final UUID playerUuid) {
        // TODO: Implement pending transaction processing when needed
    }
    
    /**
     * Calculate country economic health
     * @param countryId Country ID
     * @return Economic health score (0.0 to 1.0)
     */
    public double calculateCountryEconomicHealth(final Integer countryId) {
        try {
            final Optional<Country> countryOpt = this.plugin.getCountryManager().getCountry(countryId);
            if (countryOpt.isEmpty()) {
                return 0.0;
            }
            
            final Country country = countryOpt.get();
            final List<Player> members = this.plugin.getCountryManager().getCountryMembers(countryId);
            
            // Calculate factors
            double treasuryScore = Math.min(1.0, country.getTreasuryBalance() / 50000.0); // Max score at $50k
            double memberWealthScore = this.calculateAverageMemberWealth(members) / 10000.0; // Max score at $10k average
            double incomeScore = this.calculateCountryIncomeScore(countryId);
            double maintenanceScore = this.calculateMaintenanceScore(countryId);
            double growthScore = this.calculateGrowthScore(countryId);
            
            // Ensure scores are within bounds
            treasuryScore = Math.max(0.0, Math.min(1.0, treasuryScore));
            memberWealthScore = Math.max(0.0, Math.min(1.0, memberWealthScore));
            incomeScore = Math.max(0.0, Math.min(1.0, incomeScore));
            maintenanceScore = Math.max(0.0, Math.min(1.0, maintenanceScore));
            growthScore = Math.max(0.0, Math.min(1.0, growthScore));
            
            // Weighted average with new factors
            return (treasuryScore * 0.25) + (memberWealthScore * 0.2) + (incomeScore * 0.25) + 
                   (maintenanceScore * 0.15) + (growthScore * 0.15);
            
        } catch (final Exception exception) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to calculate economic health for country " + countryId, exception);
            return 0.0;
        }
    }
    
    /**
     * Calculate average member wealth
     * @param members Country members
     * @return Average wealth
     */
    private double calculateAverageMemberWealth(final List<Player> members) {
        if (members.isEmpty()) {
            return 0.0;
        }
        
        double totalWealth = 0.0;
        int validMembers = 0;
        
        for (final Player member : members) {
            if (this.vaultIntegration.isAvailable()) {
                final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(member.getUuid());
                totalWealth += this.vaultIntegration.getBalance(offlinePlayer);
                validMembers++;
            } else {
                totalWealth += member.getBalance();
                validMembers++;
            }
        }
        
        return validMembers > 0 ? totalWealth / validMembers : 0.0;
    }
    
    /**
     * Calculate country income score
     * @param countryId Country ID
     * @return Income score (0.0 to 1.0)
     */
    private double calculateCountryIncomeScore(final Integer countryId) {
        final double weeklyIncome = this.transactionManager.calculateCountryIncome(countryId, 7);
        final double weeklyExpenses = this.transactionManager.calculateCountryExpenses(countryId, 7);
        
        final double netIncome = weeklyIncome - weeklyExpenses;
        
        // Normalize to 0-1 scale (positive income = good)
        if (netIncome >= 0) {
            return Math.min(1.0, netIncome / 5000.0); // Max score at $5k weekly profit
        } else {
            return Math.max(0.0, 1.0 + (netIncome / 5000.0)); // Penalty for losses
        }
    }
    
    /**
     * Calculate maintenance score based on ability to pay territory maintenance
     * @param countryId Country ID
     * @return Maintenance score (0.0 to 1.0)
     */
    private double calculateMaintenanceScore(final Integer countryId) {
        final Optional<Country> countryOpt = this.plugin.getCountryManager().getCountry(countryId);
        if (countryOpt.isEmpty()) {
            return 0.0;
        }
        
        final Country country = countryOpt.get();
        final double dailyMaintenance = this.plugin.getEconomyManager().calculateDailyMaintenanceCosts(countryId);
        
        if (dailyMaintenance == 0) {
            return 1.0; // No maintenance costs
        }
        
        // Calculate how many days of maintenance the treasury can cover
        final double daysOfMaintenance = country.getTreasuryBalance() / dailyMaintenance;
        
        // Score based on days of coverage (30 days = perfect score)
        return Math.min(1.0, daysOfMaintenance / 30.0);
    }
    
    /**
     * Calculate growth score based on recent economic activity
     * @param countryId Country ID
     * @return Growth score (0.0 to 1.0)
     */
    private double calculateGrowthScore(final Integer countryId) {
        final double currentWeekIncome = this.transactionManager.calculateCountryIncome(countryId, 7);
        final double previousWeekIncome = this.transactionManager.calculateCountryIncome(countryId, 14) - currentWeekIncome;
        
        if (previousWeekIncome <= 0) {
            return currentWeekIncome > 0 ? 1.0 : 0.0;
        }
        
        final double growthRate = (currentWeekIncome - previousWeekIncome) / previousWeekIncome;
        
        // Normalize growth rate to 0-1 scale
        return Math.max(0.0, Math.min(1.0, 0.5 + (growthRate * 2.0))); // 0% growth = 0.5 score
    }
    
    /**
     * Generate economic report for a country
     * @param countryId Country ID
     * @return Economic report
     */
    public EconomicReport generateCountryEconomicReport(final Integer countryId) {
        try {
            final Optional<Country> countryOpt = this.plugin.getCountryManager().getCountry(countryId);
            if (countryOpt.isEmpty()) {
                return null;
            }
            
            final Country country = countryOpt.get();
            final List<Player> members = this.plugin.getCountryManager().getCountryMembers(countryId);
            
            return EconomicReport.builder()
                .countryId(countryId)
                .countryName(country.getName())
                .treasuryBalance(country.getTreasuryBalance())
                .memberCount(members.size())
                .averageMemberWealth(this.calculateAverageMemberWealth(members))
                .weeklyIncome(this.transactionManager.calculateCountryIncome(countryId, 7))
                .weeklyExpenses(this.transactionManager.calculateCountryExpenses(countryId, 7))
                .economicHealth(this.calculateCountryEconomicHealth(countryId))
                .taxRate(country.getTaxRate())
                .territoryCount(this.plugin.getTerritoryManager().getCountryTerritories(countryId).size())
                .territoryValue(this.plugin.getEconomyManager().calculateTerritoryValue(countryId))
                .dailyMaintenanceCost(this.plugin.getEconomyManager().calculateDailyMaintenanceCosts(countryId))
                .economicGrowthRate(this.calculateGrowthScore(countryId))
                .build();
            
        } catch (final Exception exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to generate economic report for country " + countryId, exception);
            return null;
        }
    }
    
    /**
     * Economic report data class
     */
    @lombok.Builder
    @lombok.Data
    public static class EconomicReport {
        private Integer countryId;
        private String countryName;
        private double treasuryBalance;
        private int memberCount;
        private double averageMemberWealth;
        private double weeklyIncome;
        private double weeklyExpenses;
        private double economicHealth;
        private double taxRate;
        private int territoryCount;
        private double territoryValue;
        private double dailyMaintenanceCost;
        private double economicGrowthRate;
        
        public double getNetWeeklyIncome() {
            return this.weeklyIncome - this.weeklyExpenses;
        }
        
        public String getHealthRating() {
            if (this.economicHealth >= 0.8) return "Excellent";
            if (this.economicHealth >= 0.6) return "Good";
            if (this.economicHealth >= 0.4) return "Fair";
            if (this.economicHealth >= 0.2) return "Poor";
            return "Critical";
        }
        
        public String getGrowthRating() {
            if (this.economicGrowthRate >= 0.7) return "Rapid Growth";
            if (this.economicGrowthRate >= 0.6) return "Strong Growth";
            if (this.economicGrowthRate >= 0.5) return "Stable";
            if (this.economicGrowthRate >= 0.4) return "Declining";
            return "Recession";
        }
        
        public double getMaintenanceCoverageWeeks() {
            if (this.dailyMaintenanceCost <= 0) {
                return Double.MAX_VALUE;
            }
            return this.treasuryBalance / (this.dailyMaintenanceCost * 7);
        }
    }
}