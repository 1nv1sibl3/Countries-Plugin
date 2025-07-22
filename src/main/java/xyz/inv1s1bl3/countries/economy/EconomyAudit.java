package xyz.inv1s1bl3.countries.economy;

import lombok.RequiredArgsConstructor;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.database.entities.Country;
import xyz.inv1s1bl3.countries.database.entities.Player;
import xyz.inv1s1bl3.countries.database.entities.Transaction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles economic auditing and fraud detection
 */
@RequiredArgsConstructor
public final class EconomyAudit {
    
    private final CountriesPlugin plugin;
    private final TransactionManager transactionManager;
    
    /**
     * Perform comprehensive economic audit
     * @return Audit report
     */
    public AuditReport performFullAudit() {
        this.plugin.getLogger().info("Starting comprehensive economic audit...");
        
        final AuditReport report = AuditReport.builder()
            .auditStartTime(LocalDateTime.now())
            .issues(new ArrayList<>())
            .warnings(new ArrayList<>())
            .statistics(new HashMap<>())
            .build();
        
        try {
            // Audit country treasuries
            this.auditCountryTreasuries(report);
            
            // Audit player balances
            this.auditPlayerBalances(report);
            
            // Audit transactions
            this.auditTransactions(report);
            
            // Check for suspicious activity
            this.detectSuspiciousActivity(report);
            
            // Validate data integrity
            this.validateDataIntegrity(report);
            
            report.setAuditEndTime(LocalDateTime.now());
            report.setStatus("COMPLETED");
            
            this.plugin.getLogger().info("Economic audit completed. Found " + 
                report.getIssues().size() + " issues and " + 
                report.getWarnings().size() + " warnings.");
            
        } catch (final Exception exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Error during economic audit", exception);
            report.setStatus("FAILED");
            report.getIssues().add("Audit failed due to exception: " + exception.getMessage());
        }
        
        return report;
    }
    
    /**
     * Audit country treasuries for inconsistencies
     * @param report Audit report to update
     */
    private void auditCountryTreasuries(final AuditReport report) {
        final List<Country> countries = this.plugin.getCountryManager().getAllActiveCountries();
        int treasuriesAudited = 0;
        double totalTreasuryBalance = 0.0;
        
        for (final Country country : countries) {
            treasuriesAudited++;
            totalTreasuryBalance += country.getTreasuryBalance();
            
            // Check for negative treasury balance
            if (country.getTreasuryBalance() < 0) {
                report.getIssues().add("Country '" + country.getName() + "' has negative treasury balance: $" + 
                    String.format("%.2f", country.getTreasuryBalance()));
            }
            
            // Check for unreasonably high treasury balance
            if (country.getTreasuryBalance() > 1000000) {
                report.getWarnings().add("Country '" + country.getName() + "' has very high treasury balance: $" + 
                    String.format("%.2f", country.getTreasuryBalance()));
            }
            
            // Validate treasury transactions
            final double calculatedIncome = this.transactionManager.calculateCountryIncome(country.getId(), 30);
            final double calculatedExpenses = this.transactionManager.calculateCountryExpenses(country.getId(), 30);
            
            if (calculatedIncome < 0 || calculatedExpenses < 0) {
                report.getIssues().add("Country '" + country.getName() + "' has invalid transaction calculations");
            }
        }
        
        report.getStatistics().put("treasuries_audited", treasuriesAudited);
        report.getStatistics().put("total_treasury_balance", totalTreasuryBalance);
        report.getStatistics().put("average_treasury_balance", 
            treasuriesAudited > 0 ? totalTreasuryBalance / treasuriesAudited : 0.0);
    }
    
    /**
     * Audit player balances
     * @param report Audit report to update
     */
    private void auditPlayerBalances(final AuditReport report) {
        final List<Country> countries = this.plugin.getCountryManager().getAllActiveCountries();
        int playersAudited = 0;
        double totalPlayerWealth = 0.0;
        
        for (final Country country : countries) {
            final List<Player> members = this.plugin.getCountryManager().getCountryMembers(country.getId());
            
            for (final Player member : members) {
                playersAudited++;
                final double balance = this.plugin.getEconomyManager().getPlayerBalance(member.getUuid());
                totalPlayerWealth += balance;
                
                // Check for negative balance
                if (balance < 0) {
                    report.getIssues().add("Player '" + member.getUsername() + "' has negative balance: $" + 
                        String.format("%.2f", balance));
                }
                
                // Check for unreasonably high balance
                if (balance > 500000) {
                    report.getWarnings().add("Player '" + member.getUsername() + "' has very high balance: $" + 
                        String.format("%.2f", balance));
                }
                
                // Check for balance inconsistencies with Vault
                if (this.plugin.getEconomyManager().getVaultIntegration().isAvailable()) {
                    final double vaultBalance = this.plugin.getEconomyManager().getVaultIntegration()
                        .getBalance(member.getUuid());
                    
                    if (Math.abs(balance - vaultBalance) > 0.01) {
                        report.getWarnings().add("Player '" + member.getUsername() + 
                            "' has balance mismatch between Countries ($" + String.format("%.2f", balance) + 
                            ") and Vault ($" + String.format("%.2f", vaultBalance) + ")");
                    }
                }
            }
        }
        
        report.getStatistics().put("players_audited", playersAudited);
        report.getStatistics().put("total_player_wealth", totalPlayerWealth);
        report.getStatistics().put("average_player_wealth", 
            playersAudited > 0 ? totalPlayerWealth / playersAudited : 0.0);
    }
    
    /**
     * Audit transactions for anomalies
     * @param report Audit report to update
     */
    private void auditTransactions(final AuditReport report) {
        final List<Transaction> recentTransactions = this.transactionManager.getRecentTransactions(1000);
        int transactionsAudited = recentTransactions.size();
        double totalTransactionVolume = 0.0;
        int suspiciousTransactions = 0;
        
        for (final Transaction transaction : recentTransactions) {
            totalTransactionVolume += transaction.getAmount();
            
            // Check for zero or negative amounts
            if (transaction.getAmount() <= 0) {
                report.getIssues().add("Transaction #" + transaction.getId() + 
                    " has invalid amount: $" + String.format("%.2f", transaction.getAmount()));
            }
            
            // Check for unreasonably large transactions
            if (transaction.getAmount() > 100000) {
                report.getWarnings().add("Transaction #" + transaction.getId() + 
                    " has very large amount: $" + String.format("%.2f", transaction.getAmount()));
                suspiciousTransactions++;
            }
            
            // Check for invalid transaction types
            if (transaction.getTransactionType() == null || transaction.getTransactionType().isEmpty()) {
                report.getIssues().add("Transaction #" + transaction.getId() + " has invalid transaction type");
            }
            
            // Check for orphaned transactions (invalid UUIDs/IDs)
            if (transaction.isPlayerToPlayer()) {
                if (transaction.getFromPlayerUuid() == null || transaction.getToPlayerUuid() == null) {
                    report.getIssues().add("Transaction #" + transaction.getId() + 
                        " is marked as player-to-player but has null player UUIDs");
                }
            }
            
            if (transaction.isCountryToCountry()) {
                if (transaction.getFromCountryId() == null || transaction.getToCountryId() == null) {
                    report.getIssues().add("Transaction #" + transaction.getId() + 
                        " is marked as country-to-country but has null country IDs");
                }
            }
        }
        
        report.getStatistics().put("transactions_audited", transactionsAudited);
        report.getStatistics().put("total_transaction_volume", totalTransactionVolume);
        report.getStatistics().put("suspicious_transactions", suspiciousTransactions);
        report.getStatistics().put("average_transaction_amount", 
            transactionsAudited > 0 ? totalTransactionVolume / transactionsAudited : 0.0);
    }
    
    /**
     * Detect suspicious economic activity
     * @param report Audit report to update
     */
    private void detectSuspiciousActivity(final AuditReport report) {
        // Check for rapid wealth accumulation
        this.detectRapidWealthAccumulation(report);
        
        // Check for circular transactions
        this.detectCircularTransactions(report);
        
        // Check for money laundering patterns
        this.detectMoneyLaunderingPatterns(report);
    }
    
    /**
     * Detect rapid wealth accumulation
     * @param report Audit report to update
     */
    private void detectRapidWealthAccumulation(final AuditReport report) {
        final List<Country> countries = this.plugin.getCountryManager().getAllActiveCountries();
        
        for (final Country country : countries) {
            final List<Player> members = this.plugin.getCountryManager().getCountryMembers(country.getId());
            
            for (final Player member : members) {
                final double dailyIncome = this.transactionManager.calculatePlayerIncome(member.getUuid(), 1);
                final double currentBalance = this.plugin.getEconomyManager().getPlayerBalance(member.getUuid());
                
                // Flag if daily income is more than 50% of current balance
                if (dailyIncome > 0 && currentBalance > 0 && (dailyIncome / currentBalance) > 0.5) {
                    report.getWarnings().add("Player '" + member.getUsername() + 
                        "' has rapid wealth accumulation: $" + String.format("%.2f", dailyIncome) + 
                        " in one day (current balance: $" + String.format("%.2f", currentBalance) + ")");
                }
            }
        }
    }
    
    /**
     * Detect circular transactions (potential money laundering)
     * @param report Audit report to update
     */
    private void detectCircularTransactions(final AuditReport report) {
        final List<Transaction> recentTransactions = this.transactionManager.getRecentTransactions(500);
        final Map<String, List<Transaction>> transactionChains = new HashMap<>();
        
        // Group transactions by player pairs
        for (final Transaction transaction : recentTransactions) {
            if (transaction.isPlayerToPlayer()) {
                final String key = transaction.getFromPlayerUuid() + "-" + transaction.getToPlayerUuid();
                transactionChains.computeIfAbsent(key, k -> new ArrayList<>()).add(transaction);
            }
        }
        
        // Check for rapid back-and-forth transactions
        for (final Map.Entry<String, List<Transaction>> entry : transactionChains.entrySet()) {
            final List<Transaction> transactions = entry.getValue();
            
            if (transactions.size() >= 3) {
                final double totalAmount = transactions.stream().mapToDouble(Transaction::getAmount).sum();
                final long timeSpan = java.time.Duration.between(
                    transactions.get(0).getCreatedAt(),
                    transactions.get(transactions.size() - 1).getCreatedAt()
                ).toMinutes();
                
                if (timeSpan < 60 && totalAmount > 10000) { // Multiple large transactions within an hour
                    report.getWarnings().add("Detected potential circular transactions between players: " + 
                        entry.getKey() + " ($" + String.format("%.2f", totalAmount) + " in " + timeSpan + " minutes)");
                }
            }
        }
    }
    
    /**
     * Detect money laundering patterns
     * @param report Audit report to update
     */
    private void detectMoneyLaunderingPatterns(final AuditReport report) {
        final List<Transaction> recentTransactions = this.transactionManager.getRecentTransactions(1000);
        final Map<UUID, List<Transaction>> playerTransactions = new HashMap<>();
        
        // Group transactions by player
        for (final Transaction transaction : recentTransactions) {
            if (transaction.getFromPlayerUuid() != null) {
                playerTransactions.computeIfAbsent(transaction.getFromPlayerUuid(), k -> new ArrayList<>()).add(transaction);
            }
            if (transaction.getToPlayerUuid() != null) {
                playerTransactions.computeIfAbsent(transaction.getToPlayerUuid(), k -> new ArrayList<>()).add(transaction);
            }
        }
        
        // Check for structuring (many small transactions to avoid detection)
        for (final Map.Entry<UUID, List<Transaction>> entry : playerTransactions.entrySet()) {
            final List<Transaction> transactions = entry.getValue();
            
            if (transactions.size() >= 10) {
                final double averageAmount = transactions.stream().mapToDouble(Transaction::getAmount).average().orElse(0.0);
                final long smallTransactions = transactions.stream()
                    .filter(t -> t.getAmount() < averageAmount * 0.5)
                    .count();
                
                if (smallTransactions >= 7) { // 70% of transactions are unusually small
                    final String playerName = this.plugin.getCountryManager().getPlayer(entry.getKey())
                        .map(Player::getUsername).orElse("Unknown");
                    
                    report.getWarnings().add("Player '" + playerName + 
                        "' has potential structuring pattern: " + smallTransactions + 
                        " small transactions out of " + transactions.size() + " total");
                }
            }
        }
    }
    
    /**
     * Validate data integrity
     * @param report Audit report to update
     */
    private void validateDataIntegrity(final AuditReport report) {
        // Check for orphaned records
        this.checkOrphanedRecords(report);
        
        // Validate foreign key relationships
        this.validateForeignKeys(report);
        
        // Check for data consistency
        this.checkDataConsistency(report);
    }
    
    /**
     * Check for orphaned records
     * @param report Audit report to update
     */
    private void checkOrphanedRecords(final AuditReport report) {
        // Check for players without valid countries
        final List<Country> countries = this.plugin.getCountryManager().getAllActiveCountries();
        final List<Integer> validCountryIds = countries.stream().map(Country::getId).toList();
        
        for (final Country country : countries) {
            final List<Player> members = this.plugin.getCountryManager().getCountryMembers(country.getId());
            
            for (final Player member : members) {
                if (member.hasCountry() && !validCountryIds.contains(member.getCountryId())) {
                    report.getIssues().add("Player '" + member.getUsername() + 
                        "' references non-existent country ID: " + member.getCountryId());
                }
            }
        }
    }
    
    /**
     * Validate foreign key relationships
     * @param report Audit report to update
     */
    private void validateForeignKeys(final AuditReport report) {
        final List<Transaction> recentTransactions = this.transactionManager.getRecentTransactions(500);
        
        for (final Transaction transaction : recentTransactions) {
            // Validate player UUIDs
            if (transaction.getFromPlayerUuid() != null) {
                if (this.plugin.getCountryManager().getPlayer(transaction.getFromPlayerUuid()).isEmpty()) {
                    report.getWarnings().add("Transaction #" + transaction.getId() + 
                        " references non-existent from player: " + transaction.getFromPlayerUuid());
                }
            }
            
            if (transaction.getToPlayerUuid() != null) {
                if (this.plugin.getCountryManager().getPlayer(transaction.getToPlayerUuid()).isEmpty()) {
                    report.getWarnings().add("Transaction #" + transaction.getId() + 
                        " references non-existent to player: " + transaction.getToPlayerUuid());
                }
            }
            
            // Validate country IDs
            if (transaction.getFromCountryId() != null) {
                if (this.plugin.getCountryManager().getCountry(transaction.getFromCountryId()).isEmpty()) {
                    report.getWarnings().add("Transaction #" + transaction.getId() + 
                        " references non-existent from country: " + transaction.getFromCountryId());
                }
            }
            
            if (transaction.getToCountryId() != null) {
                if (this.plugin.getCountryManager().getCountry(transaction.getToCountryId()).isEmpty()) {
                    report.getWarnings().add("Transaction #" + transaction.getId() + 
                        " references non-existent to country: " + transaction.getToCountryId());
                }
            }
        }
    }
    
    /**
     * Check data consistency
     * @param report Audit report to update
     */
    private void checkDataConsistency(final AuditReport report) {
        final List<Country> countries = this.plugin.getCountryManager().getAllActiveCountries();
        
        for (final Country country : countries) {
            // Check if leader is actually a member
            final List<Player> members = this.plugin.getCountryManager().getCountryMembers(country.getId());
            final boolean leaderIsMember = members.stream()
                .anyMatch(member -> member.getUuid().equals(country.getLeaderUuid()));
            
            if (!leaderIsMember) {
                report.getIssues().add("Country '" + country.getName() + 
                    "' leader is not a member of the country");
            }
            
            // Check tax rate validity
            if (country.getTaxRate() < 0 || country.getTaxRate() > 100) {
                report.getIssues().add("Country '" + country.getName() + 
                    "' has invalid tax rate: " + country.getTaxRate() + "%");
            }
        }
    }
    
    /**
     * Audit report data class
     */
    @lombok.Builder
    @lombok.Data
    public static class AuditReport {
        private LocalDateTime auditStartTime;
        private LocalDateTime auditEndTime;
        private String status;
        private List<String> issues;
        private List<String> warnings;
        private Map<String, Object> statistics;
        
        public long getAuditDurationMinutes() {
            if (this.auditStartTime != null && this.auditEndTime != null) {
                return java.time.Duration.between(this.auditStartTime, this.auditEndTime).toMinutes();
            }
            return 0;
        }
        
        public boolean hasIssues() {
            return this.issues != null && !this.issues.isEmpty();
        }
        
        public boolean hasWarnings() {
            return this.warnings != null && !this.warnings.isEmpty();
        }
    }
}