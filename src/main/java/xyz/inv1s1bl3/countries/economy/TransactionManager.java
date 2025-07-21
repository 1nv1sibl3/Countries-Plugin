package xyz.inv1s1bl3.countries.economy;

import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.database.entities.Transaction;
import xyz.inv1s1bl3.countries.database.repositories.TransactionRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages economic transactions and logging
 */
@RequiredArgsConstructor
public final class TransactionManager {
    
    private final CountriesPlugin plugin;
    private final TransactionRepository transactionRepository;
    
    /**
     * Record a player-to-player transaction
     * @param fromPlayerUuid Sender UUID
     * @param toPlayerUuid Receiver UUID
     * @param amount Transaction amount
     * @param description Transaction description
     * @param category Transaction category
     * @param location Transaction location (optional)
     * @return Transaction ID if successful
     */
    public Optional<Integer> recordPlayerToPlayerTransaction(final UUID fromPlayerUuid, final UUID toPlayerUuid,
                                                           final double amount, final String description,
                                                           final String category, final Location location) {
        try {
            final Transaction transaction = Transaction.builder()
                .transactionType("player_to_player")
                .fromPlayerUuid(fromPlayerUuid)
                .toPlayerUuid(toPlayerUuid)
                .amount(amount)
                .description(description)
                .category(category)
                .createdAt(LocalDateTime.now())
                .build();
            
            if (location != null) {
                transaction.setWorldName(location.getWorld().getName());
                transaction.setX(location.getBlockX());
                transaction.setY(location.getBlockY());
                transaction.setZ(location.getBlockZ());
            }
            
            final Transaction savedTransaction = this.transactionRepository.save(transaction);
            return Optional.of(savedTransaction.getId());
            
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to record player-to-player transaction", exception);
            return Optional.empty();
        }
    }
    
    /**
     * Record a player-to-country transaction
     * @param fromPlayerUuid Player UUID
     * @param toCountryId Country ID
     * @param amount Transaction amount
     * @param description Transaction description
     * @param category Transaction category
     * @param location Transaction location (optional)
     * @return Transaction ID if successful
     */
    public Optional<Integer> recordPlayerToCountryTransaction(final UUID fromPlayerUuid, final Integer toCountryId,
                                                            final double amount, final String description,
                                                            final String category, final Location location) {
        try {
            final Transaction transaction = Transaction.builder()
                .transactionType("player_to_country")
                .fromPlayerUuid(fromPlayerUuid)
                .toCountryId(toCountryId)
                .amount(amount)
                .description(description)
                .category(category)
                .createdAt(LocalDateTime.now())
                .build();
            
            if (location != null) {
                transaction.setWorldName(location.getWorld().getName());
                transaction.setX(location.getBlockX());
                transaction.setY(location.getBlockY());
                transaction.setZ(location.getBlockZ());
            }
            
            final Transaction savedTransaction = this.transactionRepository.save(transaction);
            return Optional.of(savedTransaction.getId());
            
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to record player-to-country transaction", exception);
            return Optional.empty();
        }
    }
    
    /**
     * Record a country-to-player transaction
     * @param fromCountryId Country ID
     * @param toPlayerUuid Player UUID
     * @param amount Transaction amount
     * @param description Transaction description
     * @param category Transaction category
     * @param location Transaction location (optional)
     * @return Transaction ID if successful
     */
    public Optional<Integer> recordCountryToPlayerTransaction(final Integer fromCountryId, final UUID toPlayerUuid,
                                                            final double amount, final String description,
                                                            final String category, final Location location) {
        try {
            final Transaction transaction = Transaction.builder()
                .transactionType("country_to_player")
                .fromCountryId(fromCountryId)
                .toPlayerUuid(toPlayerUuid)
                .amount(amount)
                .description(description)
                .category(category)
                .createdAt(LocalDateTime.now())
                .build();
            
            if (location != null) {
                transaction.setWorldName(location.getWorld().getName());
                transaction.setX(location.getBlockX());
                transaction.setY(location.getBlockY());
                transaction.setZ(location.getBlockZ());
            }
            
            final Transaction savedTransaction = this.transactionRepository.save(transaction);
            return Optional.of(savedTransaction.getId());
            
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to record country-to-player transaction", exception);
            return Optional.empty();
        }
    }
    
    /**
     * Record a country-to-country transaction
     * @param fromCountryId Sender country ID
     * @param toCountryId Receiver country ID
     * @param amount Transaction amount
     * @param description Transaction description
     * @param category Transaction category
     * @param location Transaction location (optional)
     * @return Transaction ID if successful
     */
    public Optional<Integer> recordCountryToCountryTransaction(final Integer fromCountryId, final Integer toCountryId,
                                                             final double amount, final String description,
                                                             final String category, final Location location) {
        try {
            final Transaction transaction = Transaction.builder()
                .transactionType("country_to_country")
                .fromCountryId(fromCountryId)
                .toCountryId(toCountryId)
                .amount(amount)
                .description(description)
                .category(category)
                .createdAt(LocalDateTime.now())
                .build();
            
            if (location != null) {
                transaction.setWorldName(location.getWorld().getName());
                transaction.setX(location.getBlockX());
                transaction.setY(location.getBlockY());
                transaction.setZ(location.getBlockZ());
            }
            
            final Transaction savedTransaction = this.transactionRepository.save(transaction);
            return Optional.of(savedTransaction.getId());
            
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to record country-to-country transaction", exception);
            return Optional.empty();
        }
    }
    
    /**
     * Get transaction history for a player
     * @param playerUuid Player UUID
     * @param limit Maximum number of transactions to return
     * @return List of transactions
     */
    public List<Transaction> getPlayerTransactionHistory(final UUID playerUuid, final int limit) {
        try {
            return this.transactionRepository.findByPlayerUuid(playerUuid, limit);
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to get player transaction history", exception);
            return List.of();
        }
    }
    
    /**
     * Get transaction history for a country
     * @param countryId Country ID
     * @param limit Maximum number of transactions to return
     * @return List of transactions
     */
    public List<Transaction> getCountryTransactionHistory(final Integer countryId, final int limit) {
        try {
            return this.transactionRepository.findByCountryId(countryId, limit);
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to get country transaction history", exception);
            return List.of();
        }
    }
    
    /**
     * Get transactions by category
     * @param category Transaction category
     * @param limit Maximum number of transactions to return
     * @return List of transactions
     */
    public List<Transaction> getTransactionsByCategory(final String category, final int limit) {
        try {
            return this.transactionRepository.findByCategory(category, limit);
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to get transactions by category", exception);
            return List.of();
        }
    }
    
    /**
     * Get recent transactions
     * @param limit Maximum number of transactions to return
     * @return List of recent transactions
     */
    public List<Transaction> getRecentTransactions(final int limit) {
        try {
            return this.transactionRepository.findRecent(limit);
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to get recent transactions", exception);
            return List.of();
        }
    }
    
    /**
     * Calculate total income for a player
     * @param playerUuid Player UUID
     * @param days Number of days to look back
     * @return Total income
     */
    public double calculatePlayerIncome(final UUID playerUuid, final int days) {
        try {
            return this.transactionRepository.calculatePlayerIncome(playerUuid, days);
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to calculate player income", exception);
            return 0.0;
        }
    }
    
    /**
     * Calculate total expenses for a player
     * @param playerUuid Player UUID
     * @param days Number of days to look back
     * @return Total expenses
     */
    public double calculatePlayerExpenses(final UUID playerUuid, final int days) {
        try {
            return this.transactionRepository.calculatePlayerExpenses(playerUuid, days);
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to calculate player expenses", exception);
            return 0.0;
        }
    }
    
    /**
     * Calculate total income for a country
     * @param countryId Country ID
     * @param days Number of days to look back
     * @return Total income
     */
    public double calculateCountryIncome(final Integer countryId, final int days) {
        try {
            return this.transactionRepository.calculateCountryIncome(countryId, days);
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to calculate country income", exception);
            return 0.0;
        }
    }
    
    /**
     * Calculate total expenses for a country
     * @param countryId Country ID
     * @param days Number of days to look back
     * @return Total expenses
     */
    public double calculateCountryExpenses(final Integer countryId, final int days) {
        try {
            return this.transactionRepository.calculateCountryExpenses(countryId, days);
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to calculate country expenses", exception);
            return 0.0;
        }
    }
}