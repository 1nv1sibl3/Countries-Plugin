package xyz.inv1s1bl3.countries;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import xyz.inv1s1bl3.countries.commands.*;
import xyz.inv1s1bl3.countries.config.ConfigManager;
import xyz.inv1s1bl3.countries.core.country.CountryManager;
import xyz.inv1s1bl3.countries.core.territory.TerritoryManager;
import xyz.inv1s1bl3.countries.core.economy.EconomyManager;
import xyz.inv1s1bl3.countries.core.market.MarketManager;
import xyz.inv1s1bl3.countries.core.diplomacy.DiplomacyManager;
import xyz.inv1s1bl3.countries.gui.GUIManager;
import xyz.inv1s1bl3.countries.listeners.PlayerListener;
import xyz.inv1s1bl3.countries.listeners.ChunkListener;
import xyz.inv1s1bl3.countries.listeners.GUIListener;
import xyz.inv1s1bl3.countries.storage.DataManager;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

/**
 * Main plugin class for the Countries plugin.
 * Manages initialization, shutdown, and provides static access to the plugin instance.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
public final class CountriesPlugin extends JavaPlugin {
    
    @Getter
    private static CountriesPlugin instance;
    
    // Core managers
    @Getter
    private ConfigManager configManager;
    @Getter
    private DataManager dataManager;
    @Getter
    private CountryManager countryManager;
    @Getter
    private TerritoryManager territoryManager;
    @Getter
    private EconomyManager economyManager;
    @Getter
    private MarketManager marketManager;
    @Getter
    private DiplomacyManager diplomacyManager;
    @Getter
    private GUIManager guiManager;
    
    @Override
    public void onEnable() {
        // Set static instance
        instance = this;
        
        // Initialize the plugin
        if (!this.initialize()) {
            this.getLogger().severe("Failed to initialize Countries plugin! Disabling...");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        this.getLogger().info("Countries plugin has been enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        // Save all data before shutdown
        if (this.dataManager != null) {
            this.dataManager.saveAllData();
        }
        
        // Cancel any running tasks
        this.getServer().getScheduler().cancelTasks(this);
        
        this.getLogger().info("Countries plugin has been disabled!");
    }
    
    /**
     * Initialize all plugin components.
     * 
     * @return true if initialization was successful, false otherwise
     */
    private boolean initialize() {
        try {
            // Initialize configuration manager
            this.configManager = new ConfigManager(this);
            if (!this.configManager.loadConfigs()) {
                return false;
            }
            
            // Initialize data manager
            this.dataManager = new DataManager(this);
            
            // Initialize core managers
            this.countryManager = new CountryManager(this);
            this.territoryManager = new TerritoryManager(this);
            this.economyManager = new EconomyManager(this);
            this.marketManager = new MarketManager(this);
            this.diplomacyManager = new DiplomacyManager(this);
            this.guiManager = new GUIManager(this);
            
            // Load data
            this.dataManager.loadAllData();
            
            // Register commands
            this.registerCommands();
            
            // Register listeners
            this.registerListeners();
            
            // Start scheduled tasks
            this.startScheduledTasks();
            
            return true;
            
        } catch (final Exception exception) {
            this.getLogger().severe("Error during plugin initialization: " + exception.getMessage());
            exception.printStackTrace();
            return false;
        }
    }
    
    /**
     * Register all plugin commands.
     */
    private void registerCommands() {
        final CountryCommand countryCommand = new CountryCommand(this);
        final TerritoryCommand territoryCommand = new TerritoryCommand(this);
        final EconomyCommand economyCommand = new EconomyCommand(this);
        final MarketCommand marketCommand = new MarketCommand(this);
        final DiplomacyCommand diplomacyCommand = new DiplomacyCommand(this);
        
        this.getCommand("country").setExecutor(countryCommand);
        this.getCommand("country").setTabCompleter(countryCommand);
        
        this.getCommand("territory").setExecutor(territoryCommand);
        this.getCommand("territory").setTabCompleter(territoryCommand);
        
        this.getCommand("economy").setExecutor(economyCommand);
        this.getCommand("economy").setTabCompleter(economyCommand);
        
        this.getCommand("market").setExecutor(marketCommand);
        this.getCommand("market").setTabCompleter(marketCommand);
        
        this.getCommand("diplomacy").setExecutor(diplomacyCommand);
        this.getCommand("diplomacy").setTabCompleter(diplomacyCommand);
    }
    
    /**
     * Register all event listeners.
     */
    private void registerListeners() {
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        this.getServer().getPluginManager().registerEvents(new ChunkListener(this), this);
        this.getServer().getPluginManager().registerEvents(new GUIListener(this), this);
    }
    
    /**
     * Start scheduled tasks for the plugin.
     */
    private void startScheduledTasks() {
        final int autoSaveInterval = this.configManager.getConfig().getInt("general.auto-save-interval", 300) * 20; // Convert to ticks
        
        // Auto-save task
        this.getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (this.dataManager != null) {
                this.dataManager.saveAllData();
            }
        }, autoSaveInterval, autoSaveInterval);
        
        // Tax collection task
        final int taxInterval = this.configManager.getConfig().getInt("economy.tax-collection-interval", 86400) * 20; // Convert to ticks
        this.getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (this.economyManager != null) {
                this.economyManager.collectTaxes();
            }
        }, taxInterval, taxInterval);
        
        // Market cleanup task (remove expired listings)
        this.getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (this.marketManager != null) {
                this.marketManager.cleanupExpiredListings();
            }
        }, 72000, 72000); // Every hour
    }
    
    /**
     * Reload the plugin configuration and data.
     * 
     * @return true if reload was successful, false otherwise
     */
    public boolean reloadPlugin() {
        try {
            // Reload configurations
            if (!this.configManager.loadConfigs()) {
                return false;
            }
            
            // Reload data
            this.dataManager.loadAllData();
            
            ChatUtils.sendMessage(this.getServer().getConsoleSender(), 
                this.configManager.getMessage("admin.reloaded"));
            
            return true;
            
        } catch (final Exception exception) {
            this.getLogger().severe("Error during plugin reload: " + exception.getMessage());
            exception.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get a message from the messages configuration.
     * 
     * @param path the message path
     * @return the formatted message
     */
    @NotNull
    public String getMessage(@NotNull final String path) {
        return this.configManager.getMessage(path);
    }
    
    /**
     * Get a message from the messages configuration with placeholders.
     * 
     * @param path the message path
     * @param placeholders the placeholders to replace
     * @return the formatted message with placeholders replaced
     */
    @NotNull
    public String getMessage(@NotNull final String path, @NotNull final String... placeholders) {
        return this.configManager.getMessage(path, placeholders);
    }
}