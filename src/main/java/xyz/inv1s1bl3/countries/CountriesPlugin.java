package xyz.inv1s1bl3.countries;

import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.inv1s1bl3.countries.config.ConfigManager;
import xyz.inv1s1bl3.countries.database.DatabaseManager;
import xyz.inv1s1bl3.countries.country.CountryManager;
import xyz.inv1s1bl3.countries.territory.TerritoryManager;
import xyz.inv1s1bl3.countries.economy.EconomyManager;
import xyz.inv1s1bl3.countries.trading.TradingManager;
import xyz.inv1s1bl3.countries.diplomacy.DiplomacyManager;
import xyz.inv1s1bl3.countries.legal.LegalManager;
import xyz.inv1s1bl3.countries.gui.GuiManager;
import xyz.inv1s1bl3.countries.chat.ChatManager;
import xyz.inv1s1bl3.countries.visualization.ParticleManager;
import xyz.inv1s1bl3.countries.commands.CommandManager;
import xyz.inv1s1bl3.countries.listeners.PlayerEventListener;
import xyz.inv1s1bl3.countries.listeners.BlockEventListener;
import xyz.inv1s1bl3.countries.listeners.ChatEventListener;
import xyz.inv1s1bl3.countries.listeners.EconomyEventListener;

/**
 * Main plugin class for Countries
 * Manages plugin lifecycle and core systems
 */
@Getter
public final class CountriesPlugin extends JavaPlugin {
    
    @Getter
    private static CountriesPlugin instance;
    
    // Core managers
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    
    // Feature managers
    private CountryManager countryManager;
    private TerritoryManager territoryManager;
    private EconomyManager economyManager;
    private TradingManager tradingManager;
    private DiplomacyManager diplomacyManager;
    private LegalManager legalManager;
    
    // UI and interaction managers
    private GuiManager guiManager;
    private ChatManager chatManager;
    private ParticleManager particleManager;
    
    // Command and event handling
    private CommandManager commandManager;
    
    // External dependencies
    private Economy vaultEconomy;
    
    @Override
    public void onEnable() {
        instance = this;
        
        this.getLogger().info("Starting Countries plugin initialization...");
        
        try {
            // Initialize core systems first
            this.initializeCore();
            
            // Initialize feature managers
            this.initializeFeatures();
            
            // Initialize UI and interaction systems
            this.initializeInteraction();
            
            // Initialize commands and listeners
            this.initializeCommandsAndListeners();
            
            // Setup external integrations
            this.setupIntegrations();
            
            this.getLogger().info("Countries plugin has been successfully enabled!");
            
        } catch (final Exception exception) {
            this.getLogger().severe("Failed to initialize Countries plugin: " + exception.getMessage());
            exception.printStackTrace();
            this.getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        this.getLogger().info("Disabling Countries plugin...");
        
        try {
            // Save all data before shutdown
            if (this.countryManager != null) {
                this.countryManager.saveAllData();
            }
            
            if (this.territoryManager != null) {
                this.territoryManager.saveAllData();
            }
            
            if (this.economyManager != null) {
                this.economyManager.saveAllData();
                this.economyManager.shutdown();
            }
            
            // Shutdown trading manager
            if (this.tradingManager != null) {
                this.tradingManager.shutdown();
            }
            
            // Close database connections
            if (this.databaseManager != null) {
                this.databaseManager.shutdown();
            }
            
            // Stop particle effects
            if (this.particleManager != null) {
                this.particleManager.stopAllEffects();
            }
            
            this.getLogger().info("Countries plugin has been disabled successfully!");
            
        } catch (final Exception exception) {
            this.getLogger().severe("Error during plugin shutdown: " + exception.getMessage());
            exception.printStackTrace();
        }
        
        instance = null;
    }
    
    /**
     * Initialize core systems (config, database)
     */
    private void initializeCore() {
        this.getLogger().info("Initializing core systems...");
        
        // Initialize configuration manager
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfigurations();
        
        // Initialize database
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.initialize();
        this.databaseManager.runMigrations();
        
        this.getLogger().info("Core systems initialized successfully!");
    }
    
    /**
     * Initialize feature managers
     */
    private void initializeFeatures() {
        this.getLogger().info("Initializing feature managers...");
        
        this.countryManager = new CountryManager(this);
        this.territoryManager = new TerritoryManager(this);
        this.economyManager = new EconomyManager(this);
        this.tradingManager = new TradingManager(this);
        this.diplomacyManager = new DiplomacyManager(this);
        this.legalManager = new LegalManager(this);
        
        // Initialize all managers
        this.countryManager.initialize();
        this.territoryManager.initialize();
        this.economyManager.initialize();
        this.tradingManager.initialize();
        this.diplomacyManager.initialize();
        this.legalManager.initialize();
        
        this.getLogger().info("Feature managers initialized successfully!");
    }
    
    /**
     * Initialize UI and interaction systems
     */
    private void initializeInteraction() {
        this.getLogger().info("Initializing interaction systems...");
        
        this.guiManager = new GuiManager(this);
        this.chatManager = new ChatManager(this);
        this.particleManager = new ParticleManager(this);
        
        this.guiManager.initialize();
        this.chatManager.initialize();
        this.particleManager.initialize();
        
        this.getLogger().info("Interaction systems initialized successfully!");
    }
    
    /**
     * Initialize commands and event listeners
     */
    private void initializeCommandsAndListeners() {
        this.getLogger().info("Initializing commands and listeners...");
        
        // Initialize command manager
        this.commandManager = new CommandManager(this);
        this.commandManager.registerCommands();
        
        // Register event listeners
        this.getServer().getPluginManager().registerEvents(new PlayerEventListener(this), this);
        this.getServer().getPluginManager().registerEvents(new BlockEventListener(this), this);
        this.getServer().getPluginManager().registerEvents(new ChatEventListener(this), this);
        this.getServer().getPluginManager().registerEvents(new EconomyEventListener(this), this);
        
        this.getLogger().info("Commands and listeners initialized successfully!");
    }
    
    /**
     * Setup external integrations (Vault, PlaceholderAPI, etc.)
     */
    private void setupIntegrations() {
        this.getLogger().info("Setting up external integrations...");
        
        // Setup Vault economy integration
        if (this.getServer().getPluginManager().getPlugin("Vault") != null) {
            final RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                this.vaultEconomy = rsp.getProvider();
                this.getLogger().info("Vault economy integration enabled!");
            } else {
                this.getLogger().warning("Vault found but no economy provider available!");
            }
        } else {
            this.getLogger().warning("Vault not found - economy features may be limited!");
        }
        
        // Setup PlaceholderAPI integration if available
        if (this.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            // TODO: Initialize PlaceholderAPI integration
            this.getLogger().info("PlaceholderAPI integration available!");
        }
        
        // Setup WorldEdit integration if available
        if (this.getServer().getPluginManager().getPlugin("WorldEdit") != null) {
            // TODO: Initialize WorldEdit integration for territory selection
            this.getLogger().info("WorldEdit integration available!");
        }
        
        // Setup Dynmap integration if available
        if (this.getServer().getPluginManager().getPlugin("dynmap") != null) {
            // TODO: Initialize Dynmap integration for territory visualization
            this.getLogger().info("Dynmap integration available!");
        }
        
        this.getLogger().info("External integrations setup complete!");
    }
    
    /**
     * Check if Vault economy is available
     * @return true if Vault economy is available
     */
    public boolean hasVaultEconomy() {
        return this.vaultEconomy != null;
    }
    
    /**
     * Reload the plugin configuration and data
     */
    public void reload() {
        this.getLogger().info("Reloading Countries plugin...");
        
        try {
            // Reload configurations
            this.configManager.loadConfigurations();
            
            // Reload managers that support it
            this.countryManager.reload();
            this.territoryManager.reload();
            this.economyManager.reload();
            
            this.getLogger().info("Countries plugin reloaded successfully!");
            
        } catch (final Exception exception) {
            this.getLogger().severe("Error during plugin reload: " + exception.getMessage());
            exception.printStackTrace();
        }
    }
}