package xyz.inv1s1bl3.countries.database.migrations;

import xyz.inv1s1bl3.countries.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Manages database schema migrations
 */
public final class MigrationManager {
    
    private final DatabaseManager databaseManager;
    private final List<Migration> migrations;
    
    public MigrationManager(final DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.migrations = new ArrayList<>();
        this.registerMigrations();
    }
    
    /**
     * Register all migrations in order
     */
    private void registerMigrations() {
        this.migrations.add(new InitialSchemaMigration());
        this.migrations.add(new CountryDataMigration());
        this.migrations.add(new TerritorySystemMigration());
        this.migrations.add(new EconomySystemMigration());
        this.migrations.add(new DiplomacySystemMigration());
        this.migrations.add(new LegalSystemMigration());
        this.migrations.add(new TradingSystemMigration());
    }
    
    /**
     * Run all pending migrations
     */
    public void runMigrations() throws SQLException {
        this.databaseManager.executeVoidOperation(connection -> {
            try {
                this.createMigrationTable(connection);
                
                for (final Migration migration : this.migrations) {
                    if (!this.isMigrationApplied(connection, migration.getName())) {
                        this.databaseManager.getPlugin().getLogger().info("Running migration: " + migration.getName());
                        migration.up(connection);
                        this.recordMigration(connection, migration.getName());
                        this.databaseManager.getPlugin().getLogger().info("Migration completed: " + migration.getName());
                    }
                }
                
            } catch (final SQLException exception) {
                this.databaseManager.getPlugin().getLogger().log(Level.SEVERE, "Migration failed!", exception);
                throw new RuntimeException("Migration execution failed", exception);
            }
        });
    }
    
    /**
     * Create the migrations tracking table
     */
    private void createMigrationTable(final Connection connection) throws SQLException {
        final String sql = """
            CREATE TABLE IF NOT EXISTS migrations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                applied_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """;
        
        try (final Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
    
    /**
     * Check if a migration has been applied
     */
    private boolean isMigrationApplied(final Connection connection, final String migrationName) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM migrations WHERE name = ?";
        
        try (final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, migrationName);
            
            try (final ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }
    
    /**
     * Record that a migration has been applied
     */
    private void recordMigration(final Connection connection, final String migrationName) throws SQLException {
        final String sql = "INSERT INTO migrations (name) VALUES (?)";
        
        try (final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, migrationName);
            statement.executeUpdate();
        }
    }
}