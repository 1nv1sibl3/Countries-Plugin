package xyz.inv1s1bl3.countries.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import xyz.inv1s1bl3.countries.CountriesPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * Manages data storage and database connections for the Countries plugin.
 * Handles SQLite database initialization and provides async data operations.
 */
public class DataManager {
    
    private final CountriesPlugin plugin;
    private final Gson gson;
    private final ExecutorService executor;
    
    private HikariDataSource dataSource;
    private CountryStorage countryStorage;
    private TerritoryStorage territoryStorage;
    
    public DataManager(CountriesPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        this.executor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                r -> {
                    Thread thread = new Thread(r);
                    thread.setName("Countries-DB-" + thread.getId());
                    thread.setDaemon(true);
                    return thread;
                }
        );
    }
    
    /**
     * Initialize database connection and create tables
     */
    public void initialize() {
        setupDatabase();
        createTables();
        initializeStorageManagers();
        
        plugin.getLogger().info("Database initialized successfully!");
    }
    
    /**
     * Setup database connection pool
     */
    private void setupDatabase() {
        HikariConfig config = new HikariConfig();
        
        String databaseType = plugin.getConfigManager().getConfig().getString("database.type", "sqlite");
        
        if ("sqlite".equalsIgnoreCase(databaseType)) {
            setupSQLite(config);
        } else {
            plugin.getLogger().severe("Unsupported database type: " + databaseType);
            throw new RuntimeException("Unsupported database type");
        }
        
        // Connection pool settings
        config.setMaximumPoolSize(plugin.getConfigManager().getConfig().getInt("database.pool.maximum-pool-size", 10));
        config.setMinimumIdle(plugin.getConfigManager().getConfig().getInt("database.pool.minimum-idle", 2));
        config.setConnectionTimeout(plugin.getConfigManager().getConfig().getLong("database.pool.connection-timeout", 30000));
        config.setIdleTimeout(plugin.getConfigManager().getConfig().getLong("database.pool.idle-timeout", 600000));
        config.setMaxLifetime(plugin.getConfigManager().getConfig().getLong("database.pool.max-lifetime", 1800000));
        
        dataSource = new HikariDataSource(config);
    }
    
    /**
     * Setup SQLite database configuration
     */
    private void setupSQLite(HikariConfig config) {
        File databaseFile = new File(plugin.getDataFolder(), 
                plugin.getConfigManager().getConfig().getString("database.sqlite.filename", "countries.db"));
        
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        
        // SQLite specific settings
        config.addDataSourceProperty("enable_load_extension", "true");
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("temp_store", "memory");
        config.addDataSourceProperty("mmap_size", "268435456"); // 256MB
    }
    
    /**
     * Create necessary database tables
     */
    private void createTables() {
        try (Connection connection = getConnection()) {
            Statement stmt = connection.createStatement();
            
            // Countries table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS countries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    owner_uuid TEXT NOT NULL,
                    founded_date INTEGER NOT NULL,
                    government_type TEXT NOT NULL DEFAULT 'MONARCHY',
                    balance REAL NOT NULL DEFAULT 0.0,
                    tax_rate REAL NOT NULL DEFAULT 0.05,
                    data TEXT NOT NULL,
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
                    updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
                )
            """);
            
            // Citizens table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS citizens (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    country_id INTEGER NOT NULL,
                    player_uuid TEXT NOT NULL,
                    role TEXT NOT NULL DEFAULT 'CITIZEN',
                    joined_date INTEGER NOT NULL,
                    salary REAL NOT NULL DEFAULT 0.0,
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
                    FOREIGN KEY (country_id) REFERENCES countries (id) ON DELETE CASCADE,
                    UNIQUE(country_id, player_uuid)
                )
            """);
            
            // Territories table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS territories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    country_id INTEGER NOT NULL,
                    world_name TEXT NOT NULL,
                    chunks TEXT NOT NULL,
                    territory_type TEXT NOT NULL DEFAULT 'RESIDENTIAL',
                    claimed_date INTEGER NOT NULL,
                    data TEXT NOT NULL,
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
                    updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
                    FOREIGN KEY (country_id) REFERENCES countries (id) ON DELETE CASCADE
                )
            """);
            
            // Economy accounts table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS economy_accounts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    account_type TEXT NOT NULL,
                    owner_uuid TEXT,
                    country_id INTEGER,
                    balance REAL NOT NULL DEFAULT 0.0,
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
                    updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
                    FOREIGN KEY (country_id) REFERENCES countries (id) ON DELETE CASCADE
                )
            """);
            
            // Transactions table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    transaction_type TEXT NOT NULL,
                    from_account_id INTEGER,
                    to_account_id INTEGER,
                    amount REAL NOT NULL,
                    description TEXT,
                    transaction_date INTEGER NOT NULL,
                    data TEXT,
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
                    FOREIGN KEY (from_account_id) REFERENCES economy_accounts (id),
                    FOREIGN KEY (to_account_id) REFERENCES economy_accounts (id)
                )
            """);
            
            // Diplomatic relations table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS diplomatic_relations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    country1_id INTEGER NOT NULL,
                    country2_id INTEGER NOT NULL,
                    relation_type TEXT NOT NULL DEFAULT 'NEUTRAL',
                    established_date INTEGER NOT NULL,
                    data TEXT,
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
                    updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
                    FOREIGN KEY (country1_id) REFERENCES countries (id) ON DELETE CASCADE,
                    FOREIGN KEY (country2_id) REFERENCES countries (id) ON DELETE CASCADE,
                    UNIQUE(country1_id, country2_id)
                )
            """);
            
            // Create indexes for better performance
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_citizens_country ON citizens (country_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_citizens_player ON citizens (player_uuid)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_territories_country ON territories (country_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_territories_world ON territories (world_name)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_accounts_type ON economy_accounts (account_type)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_accounts_owner ON economy_accounts (owner_uuid)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions (transaction_date)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_relations_countries ON diplomatic_relations (country1_id, country2_id)");
            
            plugin.debug("Database tables created/verified successfully");
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create database tables!", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    /**
     * Initialize storage managers
     */
    private void initializeStorageManagers() {
        countryStorage = new CountryStorage(this);
        territoryStorage = new TerritoryStorage(this);
    }
    
    /**
     * Get a database connection from the pool
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    /**
     * Execute a database operation asynchronously
     */
    public CompletableFuture<Void> executeAsync(Runnable task) {
        return CompletableFuture.runAsync(task, executor);
    }
    
    /**
     * Execute a database operation asynchronously and return a result
     */
    public <T> CompletableFuture<T> supplyAsync(java.util.function.Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }
    
    /**
     * Save all data to database
     */
    public void saveAll() {
        executeAsync(() -> {
            try {
                plugin.debug("Starting save operation for all data");
                
                if (countryStorage != null) {
                    countryStorage.saveAll();
                }
                
                if (territoryStorage != null) {
                    territoryStorage.saveAll();
                }
                
                plugin.debug("Save operation completed successfully");
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error occurred during save operation!", e);
            }
        });
    }
    
    /**
     * Shutdown the data manager
     */
    public void shutdown() {
        plugin.getLogger().info("Shutting down data manager...");
        
        try {
            // Save all data one final time synchronously
            if (countryStorage != null) {
                countryStorage.saveAll();
            }
            
            if (territoryStorage != null) {
                territoryStorage.saveAll();
            }
            
            // Shutdown executor
            executor.shutdown();
            
            // Close database connection pool
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
            }
            
            plugin.getLogger().info("Data manager shutdown complete!");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred during data manager shutdown!", e);
        }
    }
    
    // Getters
    
    public Gson getGson() {
        return gson;
    }
    
    public ExecutorService getExecutor() {
        return executor;
    }
    
    public CountryStorage getCountryStorage() {
        return countryStorage;
    }
    
    public TerritoryStorage getTerritoryStorage() {
        return territoryStorage;
    }
    
    public CountriesPlugin getPlugin() {
        return plugin;
    }
}