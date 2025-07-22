package xyz.inv1s1bl3.countries.database.repositories;

import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.database.entities.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for transaction data operations
 */
public final class TransactionRepository implements Repository<Transaction, Integer> {
    
    private final CountriesPlugin plugin;
    
    public TransactionRepository(final CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public Transaction save(final Transaction transaction) throws SQLException {
        final String sql = """
            INSERT INTO transactions (transaction_type, from_player_uuid, to_player_uuid, from_country_id,
                                    to_country_id, amount, description, category, created_at, world_name, x, y, z)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, transaction.getTransactionType());
                
                if (transaction.getFromPlayerUuid() != null) {
                    statement.setString(2, transaction.getFromPlayerUuid().toString());
                } else {
                    statement.setNull(2, java.sql.Types.VARCHAR);
                }
                
                if (transaction.getToPlayerUuid() != null) {
                    statement.setString(3, transaction.getToPlayerUuid().toString());
                } else {
                    statement.setNull(3, java.sql.Types.VARCHAR);
                }
                
                if (transaction.getFromCountryId() != null) {
                    statement.setInt(4, transaction.getFromCountryId());
                } else {
                    statement.setNull(4, java.sql.Types.INTEGER);
                }
                
                if (transaction.getToCountryId() != null) {
                    statement.setInt(5, transaction.getToCountryId());
                } else {
                    statement.setNull(5, java.sql.Types.INTEGER);
                }
                
                statement.setDouble(6, transaction.getAmount());
                statement.setString(7, transaction.getDescription());
                statement.setString(8, transaction.getCategory());
                statement.setString(9, transaction.getCreatedAt().toString());
                statement.setString(10, transaction.getWorldName());
                
                if (transaction.getX() != null) {
                    statement.setInt(11, transaction.getX());
                } else {
                    statement.setNull(11, java.sql.Types.INTEGER);
                }
                
                if (transaction.getY() != null) {
                    statement.setInt(12, transaction.getY());
                } else {
                    statement.setNull(12, java.sql.Types.INTEGER);
                }
                
                if (transaction.getZ() != null) {
                    statement.setInt(13, transaction.getZ());
                } else {
                    statement.setNull(13, java.sql.Types.INTEGER);
                }
                
                statement.executeUpdate();
                
                try (final ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        transaction.setId(generatedKeys.getInt(1));
                    }
                }
                
                return transaction;
            }
        });
    }
    
    @Override
    public Transaction update(final Transaction transaction) throws SQLException {
        final String sql = """
            UPDATE transactions SET description = ?, category = ?
            WHERE id = ?
            """;
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, transaction.getDescription());
                statement.setString(2, transaction.getCategory());
                statement.setInt(3, transaction.getId());
                
                statement.executeUpdate();
                return transaction;
            }
        });
    }
    
    @Override
    public Optional<Transaction> findById(final Integer id) throws SQLException {
        final String sql = "SELECT * FROM transactions WHERE id = ?";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(this.mapResultSetToTransaction(resultSet));
                    }
                    return Optional.empty();
                }
            }
        });
    }
    
    @Override
    public List<Transaction> findAll() throws SQLException {
        final String sql = "SELECT * FROM transactions ORDER BY created_at DESC";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            final List<Transaction> transactions = new ArrayList<>();
            
            try (final Statement statement = connection.createStatement();
                 final ResultSet resultSet = statement.executeQuery(sql)) {
                
                while (resultSet.next()) {
                    transactions.add(this.mapResultSetToTransaction(resultSet));
                }
            }
            
            return transactions;
        });
    }
    
    @Override
    public boolean deleteById(final Integer id) throws SQLException {
        final String sql = "DELETE FROM transactions WHERE id = ?";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                return statement.executeUpdate() > 0;
            }
        });
    }
    
    @Override
    public boolean existsById(final Integer id) throws SQLException {
        final String sql = "SELECT 1 FROM transactions WHERE id = ? LIMIT 1";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            }
        });
    }
    
    @Override
    public long count() throws SQLException {
        final String sql = "SELECT COUNT(*) FROM transactions";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final Statement statement = connection.createStatement();
                 final ResultSet resultSet = statement.executeQuery(sql)) {
                
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
                return 0L;
            }
        });
    }
    
    /**
     * Find transactions by player UUID
     * @param playerUuid Player UUID
     * @param limit Maximum number of transactions
     * @return List of transactions
     * @throws SQLException if query fails
     */
    public List<Transaction> findByPlayerUuid(final UUID playerUuid, final int limit) throws SQLException {
        final String sql = """
            SELECT * FROM transactions 
            WHERE from_player_uuid = ? OR to_player_uuid = ?
            ORDER BY created_at DESC
            LIMIT ?
            """;
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            final List<Transaction> transactions = new ArrayList<>();
            
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerUuid.toString());
                statement.setString(2, playerUuid.toString());
                statement.setInt(3, limit);
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        transactions.add(this.mapResultSetToTransaction(resultSet));
                    }
                }
            }
            
            return transactions;
        });
    }
    
    /**
     * Find transactions by country ID
     * @param countryId Country ID
     * @param limit Maximum number of transactions
     * @return List of transactions
     * @throws SQLException if query fails
     */
    public List<Transaction> findByCountryId(final Integer countryId, final int limit) throws SQLException {
        final String sql = """
            SELECT * FROM transactions 
            WHERE from_country_id = ? OR to_country_id = ?
            ORDER BY created_at DESC
            LIMIT ?
            """;
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            final List<Transaction> transactions = new ArrayList<>();
            
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, countryId);
                statement.setInt(2, countryId);
                statement.setInt(3, limit);
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        transactions.add(this.mapResultSetToTransaction(resultSet));
                    }
                }
            }
            
            return transactions;
        });
    }
    
    /**
     * Find transactions by category
     * @param category Transaction category
     * @param limit Maximum number of transactions
     * @return List of transactions
     * @throws SQLException if query fails
     */
    public List<Transaction> findByCategory(final String category, final int limit) throws SQLException {
        final String sql = """
            SELECT * FROM transactions 
            WHERE category = ?
            ORDER BY created_at DESC
            LIMIT ?
            """;
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            final List<Transaction> transactions = new ArrayList<>();
            
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, category);
                statement.setInt(2, limit);
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        transactions.add(this.mapResultSetToTransaction(resultSet));
                    }
                }
            }
            
            return transactions;
        });
    }
    
    /**
     * Find recent transactions
     * @param limit Maximum number of transactions
     * @return List of transactions
     * @throws SQLException if query fails
     */
    public List<Transaction> findRecent(final int limit) throws SQLException {
        final String sql = """
            SELECT * FROM transactions 
            ORDER BY created_at DESC
            LIMIT ?
            """;
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            final List<Transaction> transactions = new ArrayList<>();
            
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, limit);
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        transactions.add(this.mapResultSetToTransaction(resultSet));
                    }
                }
            }
            
            return transactions;
        });
    }
    
    /**
     * Calculate player income over a period
     * @param playerUuid Player UUID
     * @param days Number of days to look back
     * @return Total income
     * @throws SQLException if query fails
     */
    public double calculatePlayerIncome(final UUID playerUuid, final int days) throws SQLException {
        final String sql = """
            SELECT COALESCE(SUM(amount), 0) FROM transactions 
            WHERE to_player_uuid = ? AND created_at >= datetime('now', '-' || ? || ' days')
            """;
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerUuid.toString());
                statement.setInt(2, days);
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getDouble(1);
                    }
                    return 0.0;
                }
            }
        });
    }
    
    /**
     * Calculate player expenses over a period
     * @param playerUuid Player UUID
     * @param days Number of days to look back
     * @return Total expenses
     * @throws SQLException if query fails
     */
    public double calculatePlayerExpenses(final UUID playerUuid, final int days) throws SQLException {
        final String sql = """
            SELECT COALESCE(SUM(amount), 0) FROM transactions 
            WHERE from_player_uuid = ? AND created_at >= datetime('now', '-' || ? || ' days')
            """;
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerUuid.toString());
                statement.setInt(2, days);
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getDouble(1);
                    }
                    return 0.0;
                }
            }
        });
    }
    
    /**
     * Calculate country income over a period
     * @param countryId Country ID
     * @param days Number of days to look back
     * @return Total income
     * @throws SQLException if query fails
     */
    public double calculateCountryIncome(final Integer countryId, final int days) throws SQLException {
        final String sql = """
            SELECT COALESCE(SUM(amount), 0) FROM transactions 
            WHERE to_country_id = ? AND created_at >= datetime('now', '-' || ? || ' days')
            """;
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, countryId);
                statement.setInt(2, days);
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getDouble(1);
                    }
                    return 0.0;
                }
            }
        });
    }
    
    /**
     * Calculate country expenses over a period
     * @param countryId Country ID
     * @param days Number of days to look back
     * @return Total expenses
     * @throws SQLException if query fails
     */
    public double calculateCountryExpenses(final Integer countryId, final int days) throws SQLException {
        final String sql = """
            SELECT COALESCE(SUM(amount), 0) FROM transactions 
            WHERE from_country_id = ? AND created_at >= datetime('now', '-' || ? || ' days')
            """;
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, countryId);
                statement.setInt(2, days);
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getDouble(1);
                    }
                    return 0.0;
                }
            }
        });
    }
    
    /**
     * Map ResultSet to Transaction entity
     */
    private Transaction mapResultSetToTransaction(final ResultSet resultSet) throws SQLException {
        final String fromPlayerUuidStr = resultSet.getString("from_player_uuid");
        final String toPlayerUuidStr = resultSet.getString("to_player_uuid");
        final String fromCountryIdStr = resultSet.getString("from_country_id");
        final String toCountryIdStr = resultSet.getString("to_country_id");
        final String xStr = resultSet.getString("x");
        final String yStr = resultSet.getString("y");
        final String zStr = resultSet.getString("z");
        
        return Transaction.builder()
                .id(resultSet.getInt("id"))
                .transactionType(resultSet.getString("transaction_type"))
                .fromPlayerUuid(fromPlayerUuidStr != null ? UUID.fromString(fromPlayerUuidStr) : null)
                .toPlayerUuid(toPlayerUuidStr != null ? UUID.fromString(toPlayerUuidStr) : null)
                .fromCountryId(fromCountryIdStr != null ? Integer.parseInt(fromCountryIdStr) : null)
                .toCountryId(toCountryIdStr != null ? Integer.parseInt(toCountryIdStr) : null)
                .amount(resultSet.getDouble("amount"))
                .description(resultSet.getString("description"))
                .category(resultSet.getString("category"))
                .createdAt(LocalDateTime.parse(resultSet.getString("created_at")))
                .worldName(resultSet.getString("world_name"))
                .x(xStr != null ? Integer.parseInt(xStr) : null)
                .y(yStr != null ? Integer.parseInt(yStr) : null)
                .z(zStr != null ? Integer.parseInt(zStr) : null)
                .build();
    }
}