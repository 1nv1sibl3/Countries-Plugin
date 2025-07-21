package xyz.inv1s1bl3.countries.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.database.migrations.MigrationManager;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Manages database connections and operations
 */
@Getter
public final class DatabaseManager {
    
    private final CountriesPlugin plugin;
    private HikariDataSource dataSource;
    private MigrationManager migrationManager;
    
    public DatabaseManager(final CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Initialize the database connection pool
     */
    public void initialize() {
        this.plugin.getLogger().info("Initializing database connection...");
        
        try {
            this.setupDataSource();
            this.migrationManager = new MigrationManager(this);
            
            // Test connection
            try (final Connection connection = this.getConnection()) {
                if (connection != null && !connection.isClosed()) {
                    this.plugin.getLogger().info("Database connection established successfully!");
                } else {
                    throw new SQLException("Failed to establish database connection");
                }
            }
            
        } catch (final Exception exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to initialize database!", exception);
            throw new RuntimeException("Database initialization failed", exception);
        }
    }
    
    /**
     * Setup HikariCP data source
     */
    private void setupDataSource() {
        final HikariConfig config = new HikariConfig();
        
        // Database file path
        final File dataFolder = this.plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        final String databaseFile = this.plugin.getConfigManager().getDatabaseFile();
        final File dbFile = new File(dataFolder, databaseFile);
        
        // SQLite connection URL
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        
        // Connection pool settings
        final int maxPoolSize = this.plugin.getConfigManager().getMainConfig()
                .getInt("database.pool.maximum-pool-size", 10);
        final int minIdle = this.plugin.getConfigManager().getMainConfig()
                .getInt("database.pool.minimum-idle", 2);
        final long connectionTimeout = this.plugin.getConfigManager().getMainConfig()
                .getLong("database.pool.connection-timeout", 30000);
        final long idleTimeout = this.plugin.getConfigManager().getMainConfig()
                .getLong("database.pool.idle-timeout", 600000);
        final long maxLifetime = this.plugin.getConfigManager().getMainConfig()
                .getLong("database.pool.max-lifetime", 1800000);
        
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        
        // SQLite specific settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        this.dataSource = new HikariDataSource(config);
        
        this.plugin.getLogger().info("Database connection pool configured successfully!");
    }
    
    /**
     * Get a database connection from the pool
     * @return Database connection
     * @throws SQLException if connection fails
     */
    public Connection getConnection() throws SQLException {
        if (this.dataSource == null || this.dataSource.isClosed()) {
            throw new SQLException("Database connection pool is not available");
        }
        return this.dataSource.getConnection();
    }
    
    /**
     * Run database migrations
     */
    public void runMigrations() {
        this.plugin.getLogger().info("Running database migrations...");
        
        try {
            this.migrationManager.runMigrations();
            this.plugin.getLogger().info("Database migrations completed successfully!");
            
        } catch (final Exception exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to run database migrations!", exception);
            throw new RuntimeException("Migration failed", exception);
        }
    }
    
    /**
     * Shutdown the database connection pool
     */
    public void shutdown() {
        this.plugin.getLogger().info("Shutting down database connection pool...");
        
        if (this.dataSource != null && !this.dataSource.isClosed()) {
            this.dataSource.close();
            this.plugin.getLogger().info("Database connection pool closed successfully!");
        }
    }
    
    /**
     * Check if the database is available
     * @return true if database is available
     */
    public boolean isAvailable() {
        return this.dataSource != null && !this.dataSource.isClosed();
    }
    
    /**
     * Execute a database operation with automatic connection management
     * @param operation Database operation to execute
     * @param <T> Return type
     * @return Operation result
     * @throws SQLException if operation fails
     */
    public <T> T executeOperation(final DatabaseOperation<T> operation) throws SQLException {
        try (final Connection connection = this.getConnection()) {
            return operation.execute(connection);
        }
    }
    
    /**
     * Execute a database operation without return value
     * @param operation Database operation to execute
     * @throws SQLException if operation fails
     */
    public void executeVoidOperation(final VoidDatabaseOperation operation) throws SQLException {
        try (final Connection connection = this.getConnection()) {
            operation.execute(connection);
        }
    }
    
    /**
     * Functional interface for database operations with return value
     */
    @FunctionalInterface
    public interface DatabaseOperation<T> {
        T execute(Connection connection) throws SQLException;
    }
    
    /**
     * Functional interface for database operations without return value
     */
    @FunctionalInterface
    public interface VoidDatabaseOperation {
        void execute(Connection connection) throws SQLException;
    }
}