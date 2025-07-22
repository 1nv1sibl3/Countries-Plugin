package xyz.inv1s1bl3.countries.economy;

import lombok.RequiredArgsConstructor;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.database.entities.Country;
import xyz.inv1s1bl3.countries.database.entities.Player;
import xyz.inv1s1bl3.countries.database.entities.Transaction;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles economic statistics and analytics
 */
@RequiredArgsConstructor
public final class EconomyStatistics {
    
    private final CountriesPlugin plugin;
    private final TransactionManager transactionManager;
    
    /**
     * Generate global economic statistics
     * @return Global economic stats
     */
    public GlobalEconomicStats generateGlobalStats() {
        final List<Country> countries = this.plugin.getCountryManager().getAllActiveCountries();
        
        double totalTreasuryBalance = 0.0;
        double totalPlayerWealth = 0.0;
        int totalTransactions = 0;
        double totalTransactionVolume = 0.0;
        
        for (final Country country : countries) {
            totalTreasuryBalance += country.getTreasuryBalance();
            
            final List<Player> members = this.plugin.getCountryManager().getCountryMembers(country.getId());
            for (final Player member : members) {
                totalPlayerWealth += this.plugin.getEconomyManager().getPlayerBalance(member.getUuid());
            }
            
            final List<Transaction> countryTransactions = this.transactionManager.getCountryTransactionHistory(country.getId(), 1000);
            totalTransactions += countryTransactions.size();
            totalTransactionVolume += countryTransactions.stream().mapToDouble(Transaction::getAmount).sum();
        }
        
        return GlobalEconomicStats.builder()
            .totalCountries(countries.size())
            .totalTreasuryBalance(totalTreasuryBalance)
            .totalPlayerWealth(totalPlayerWealth)
            .totalEconomicValue(totalTreasuryBalance + totalPlayerWealth)
            .totalTransactions(totalTransactions)
            .totalTransactionVolume(totalTransactionVolume)
            .averageTreasuryBalance(countries.isEmpty() ? 0.0 : totalTreasuryBalance / countries.size())
            .generatedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * Generate player economic statistics
     * @param playerUuid Player UUID
     * @return Player economic stats
     */
    public PlayerEconomicStats generatePlayerStats(final UUID playerUuid) {
        final double currentBalance = this.plugin.getEconomyManager().getPlayerBalance(playerUuid);
        final double weeklyIncome = this.transactionManager.calculatePlayerIncome(playerUuid, 7);
        final double weeklyExpenses = this.transactionManager.calculatePlayerExpenses(playerUuid, 7);
        final double monthlyIncome = this.transactionManager.calculatePlayerIncome(playerUuid, 30);
        final double monthlyExpenses = this.transactionManager.calculatePlayerExpenses(playerUuid, 30);
        
        final List<Transaction> recentTransactions = this.transactionManager.getPlayerTransactionHistory(playerUuid, 50);
        final Map<String, Integer> transactionsByCategory = recentTransactions.stream()
            .collect(Collectors.groupingBy(
                Transaction::getCategory,
                Collectors.summingInt(t -> 1)
            ));
        
        return PlayerEconomicStats.builder()
            .playerUuid(playerUuid)
            .currentBalance(currentBalance)
            .weeklyIncome(weeklyIncome)
            .weeklyExpenses(weeklyExpenses)
            .weeklyNetIncome(weeklyIncome - weeklyExpenses)
            .monthlyIncome(monthlyIncome)
            .monthlyExpenses(monthlyExpenses)
            .monthlyNetIncome(monthlyIncome - monthlyExpenses)
            .totalTransactions(recentTransactions.size())
            .transactionsByCategory(transactionsByCategory)
            .generatedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * Generate country economic statistics
     * @param countryId Country ID
     * @return Country economic stats
     */
    public CountryEconomicStats generateCountryStats(final Integer countryId) {
        final Country country = this.plugin.getCountryManager().getCountry(countryId).orElse(null);
        if (country == null) {
            return null;
        }
        
        final List<Player> members = this.plugin.getCountryManager().getCountryMembers(countryId);
        final double totalMemberWealth = members.stream()
            .mapToDouble(member -> this.plugin.getEconomyManager().getPlayerBalance(member.getUuid()))
            .sum();
        
        final double weeklyIncome = this.transactionManager.calculateCountryIncome(countryId, 7);
        final double weeklyExpenses = this.transactionManager.calculateCountryExpenses(countryId, 7);
        final double monthlyIncome = this.transactionManager.calculateCountryIncome(countryId, 30);
        final double monthlyExpenses = this.transactionManager.calculateCountryExpenses(countryId, 30);
        
        final List<Transaction> recentTransactions = this.transactionManager.getCountryTransactionHistory(countryId, 100);
        final Map<String, Double> expensesByCategory = recentTransactions.stream()
            .filter(t -> t.getFromCountryId() != null && t.getFromCountryId().equals(countryId))
            .collect(Collectors.groupingBy(
                Transaction::getCategory,
                Collectors.summingDouble(Transaction::getAmount)
            ));
        
        final Map<String, Double> incomeByCategory = recentTransactions.stream()
            .filter(t -> t.getToCountryId() != null && t.getToCountryId().equals(countryId))
            .collect(Collectors.groupingBy(
                Transaction::getCategory,
                Collectors.summingDouble(Transaction::getAmount)
            ));
        
        return CountryEconomicStats.builder()
            .countryId(countryId)
            .countryName(country.getName())
            .treasuryBalance(country.getTreasuryBalance())
            .totalMemberWealth(totalMemberWealth)
            .totalEconomicValue(country.getTreasuryBalance() + totalMemberWealth)
            .memberCount(members.size())
            .averageMemberWealth(members.isEmpty() ? 0.0 : totalMemberWealth / members.size())
            .weeklyIncome(weeklyIncome)
            .weeklyExpenses(weeklyExpenses)
            .weeklyNetIncome(weeklyIncome - weeklyExpenses)
            .monthlyIncome(monthlyIncome)
            .monthlyExpenses(monthlyExpenses)
            .monthlyNetIncome(monthlyIncome - monthlyExpenses)
            .taxRate(country.getTaxRate())
            .expensesByCategory(expensesByCategory)
            .incomeByCategory(incomeByCategory)
            .generatedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * Get top countries by economic metrics
     * @param metric Metric to sort by
     * @param limit Number of countries to return
     * @return List of countries sorted by metric
     */
    public List<CountryEconomicRanking> getTopCountriesByMetric(final EconomicMetric metric, final int limit) {
        final List<Country> countries = this.plugin.getCountryManager().getAllActiveCountries();
        
        return countries.stream()
            .map(country -> {
                final CountryEconomicStats stats = this.generateCountryStats(country.getId());
                final double value = this.getMetricValue(stats, metric);
                
                return CountryEconomicRanking.builder()
                    .countryId(country.getId())
                    .countryName(country.getName())
                    .metric(metric)
                    .value(value)
                    .build();
            })
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Get metric value from country stats
     * @param stats Country stats
     * @param metric Metric to extract
     * @return Metric value
     */
    private double getMetricValue(final CountryEconomicStats stats, final EconomicMetric metric) {
        if (stats == null) {
            return 0.0;
        }
        
        return switch (metric) {
            case TREASURY_BALANCE -> stats.getTreasuryBalance();
            case TOTAL_WEALTH -> stats.getTotalEconomicValue();
            case WEEKLY_INCOME -> stats.getWeeklyIncome();
            case WEEKLY_NET_INCOME -> stats.getWeeklyNetIncome();
            case AVERAGE_MEMBER_WEALTH -> stats.getAverageMemberWealth();
            case MEMBER_COUNT -> stats.getMemberCount();
        };
    }
    
    /**
     * Economic metrics enum
     */
    public enum EconomicMetric {
        TREASURY_BALANCE,
        TOTAL_WEALTH,
        WEEKLY_INCOME,
        WEEKLY_NET_INCOME,
        AVERAGE_MEMBER_WEALTH,
        MEMBER_COUNT
    }
    
    /**
     * Global economic statistics
     */
    @lombok.Builder
    @lombok.Data
    public static class GlobalEconomicStats {
        private int totalCountries;
        private double totalTreasuryBalance;
        private double totalPlayerWealth;
        private double totalEconomicValue;
        private int totalTransactions;
        private double totalTransactionVolume;
        private double averageTreasuryBalance;
        private LocalDateTime generatedAt;
    }
    
    /**
     * Player economic statistics
     */
    @lombok.Builder
    @lombok.Data
    public static class PlayerEconomicStats {
        private UUID playerUuid;
        private double currentBalance;
        private double weeklyIncome;
        private double weeklyExpenses;
        private double weeklyNetIncome;
        private double monthlyIncome;
        private double monthlyExpenses;
        private double monthlyNetIncome;
        private int totalTransactions;
        private Map<String, Integer> transactionsByCategory;
        private LocalDateTime generatedAt;
    }
    
    /**
     * Country economic statistics
     */
    @lombok.Builder
    @lombok.Data
    public static class CountryEconomicStats {
        private Integer countryId;
        private String countryName;
        private double treasuryBalance;
        private double totalMemberWealth;
        private double totalEconomicValue;
        private int memberCount;
        private double averageMemberWealth;
        private double weeklyIncome;
        private double weeklyExpenses;
        private double weeklyNetIncome;
        private double monthlyIncome;
        private double monthlyExpenses;
        private double monthlyNetIncome;
        private double taxRate;
        private int territoryCount;
        private double territoryValue;
        private double dailyMaintenanceCost;
        private Map<String, Double> expensesByCategory;
        private Map<String, Double> incomeByCategory;
        private LocalDateTime generatedAt;
    }
    
    /**
     * Country economic ranking
     */
    @lombok.Builder
    @lombok.Data
    public static class CountryEconomicRanking {
        private Integer countryId;
        private String countryName;
        private EconomicMetric metric;
        private double value;
    }
}