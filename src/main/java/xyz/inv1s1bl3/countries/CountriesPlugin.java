package xyz.inv1s1bl3.countries;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.inv1s1bl3.countries.api.CountriesAPI;
import xyz.inv1s1bl3.countries.commands.CountryCommand;
import xyz.inv1s1bl3.countries.commands.EconomyCommand;
import xyz.inv1s1bl3.countries.commands.DiplomacyCommand;
import xyz.inv1s1bl3.countries.commands.AdminCommand;
import xyz.inv1s1bl3.countries.config.ConfigManager;
import xyz.inv1s1bl3.countries.core.country.CountryManager;
import xyz.inv1s1bl3.countries.core.territory.TerritoryManager;
import xyz.inv1s1bl3.countries.core.economy.EconomyManager;
import xyz.inv1s1bl3.countries.core.diplomacy.DiplomacyManager;
import xyz.inv1s1bl3.countries.core.law.LawSystem;
import xyz.inv1s1bl3.countries.commands.TerritoryCommand;
import xyz.inv1s1bl3.countries.commands.LawCommand;
import xyz.inv1s1bl3.countries.gui.GUIListener;
import xyz.inv1s1bl3.countries.gui.TerritoryGUIListener;
import xyz.inv1s1bl3.countries.listeners.ChunkListener;
import xyz.inv1s1bl3.countries.listeners.PlayerListener;
import xyz.inv1s1bl3.countries.listeners.SelectionToolListener;
import xyz.inv1s1bl3.countries.storage.DataManager;
import xyz.inv1s1bl3.countries.utils.PerformanceMonitor;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.logging.Level;

/**
 * Main plugin class for the Countries plugin.
 * Handles initialization, shutdown, and provides access to all major components.
 */
public final class CountriesPlugin extends JavaPlugin {

    private static CountriesPlugin instance;
    
    // Core managers
    private ConfigManager configManager;
    private DataManager dataManager;
    private CountryManager countryManager;
    private TerritoryManager territoryManager;
    private EconomyManager economyManager;
    private DiplomacyManager diplomacyManager;
    private LawSystem lawSystem;
    
    // API
    private CountriesAPI api;
    
    // GUI Listeners
    private GUIListener guiListener;
    private TerritoryGUIListener territoryGUIListener;
    private SelectionToolListener selectionToolListener;
    
    // Performance monitoring
    private PerformanceMonitor performanceMonitor;
    
    // Vault economy
    private Economy vaultEconomy;
    
    // Auto-save task
    private BukkitRunnable autoSaveTask;
    
    @Override
    public void onLoad() {
        instance = this;
        getLogger().info("Countries plugin is loading...");
    }

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        
        try {
            // Initialize configuration
            initializeConfig();
            
            // Initialize database
            initializeDatabase();
            
            // Setup Vault economy
            setupEconomy();
            
            // Initialize managers
            initializeManagers();
            
            // Register commands
            registerCommands();
            
            // Register listeners
            registerListeners();
            
            // Start auto-save task
            startAutoSaveTask();
            
            // Initialize API
            api = new CountriesAPI(this);
            
            // Initialize advanced features
            initializeAdvancedFeatures();
            
            long loadTime = System.currentTimeMillis() - startTime;
            getLogger().info(String.format("Countries plugin enabled successfully in %dms!", loadTime));
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable Countries plugin!", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Countries plugin is disabling...");
        
        try {
            // Stop auto-save task
            if (autoSaveTask != null) {
                autoSaveTask.cancel();
                autoSaveTask = null;
            }
            
            // Save all data
            if (dataManager != null) {
                dataManager.saveAll();
            }
            
            // Shutdown managers
            if (dataManager != null) {
                dataManager.shutdown();
            }
            
            if (economyManager != null) {
                economyManager.shutdown();
            }
            
            if (diplomacyManager != null) {
                // Diplomacy manager doesn't need explicit shutdown currently
            }
            
            if (lawSystem != null) {
                // Law system doesn't need explicit shutdown currently
            }
            
            getLogger().info("Countries plugin disabled successfully!");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error occurred while disabling Countries plugin!", e);
        } finally {
            instance = null;
        }
    }
    
    /**
     * Initialize configuration files
     */
    private void initializeConfig() {
        getLogger().info("Initializing configuration...");
        configManager = new ConfigManager(this);
        configManager.loadConfigs();
    }
    
    /**
     * Initialize database connection
     */
    private void initializeDatabase() {
        getLogger().info("Initializing database...");
        dataManager = new DataManager(this);
        dataManager.initialize();
    }
    
    /**
     * Setup Vault economy integration
     */
    private void setupEconomy() {
        if (!getServer().getPluginManager().isPluginEnabled("Vault")) {
            getLogger().warning("Vault not found! Economy features will be limited.");
            return;
        }
        
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("No economy plugin found! Economy features will be limited.");
            return;
        }
        
        vaultEconomy = rsp.getProvider();
        getLogger().info("Hooked into " + vaultEconomy.getName() + " for economy support!");
    }
    
    /**
     * Initialize all managers
     */
    private void initializeManagers() {
        getLogger().info("Initializing managers...");
        
        countryManager = new CountryManager(this);
        territoryManager = new TerritoryManager(this);
        economyManager = new EconomyManager(this);
        diplomacyManager = new DiplomacyManager(this);
        lawSystem = new LawSystem(this);
        
        // Load data
        countryManager.loadCountries();
        territoryManager.loadTerritories();
        economyManager.initialize();
        diplomacyManager.initialize();
        lawSystem.initialize();
    }
    
    /**
     * Register all commands
     */
    private void registerCommands() {
        getLogger().info("Registering commands...");
        
        getCommand("countries").setExecutor(new CountryCommand(this));
        getCommand("territory").setExecutor(new TerritoryCommand(this));
        getCommand("ceconomy").setExecutor(new EconomyCommand(this));
        getCommand("diplomacy").setExecutor(new DiplomacyCommand(this));
        getCommand("law").setExecutor(new LawCommand(this));
        getCommand("cadmin").setExecutor(new AdminCommand(this));
    }
    
    /**
     * Register all event listeners
     */
    private void registerListeners() {
        getLogger().info("Registering event listeners...");
        
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new ChunkListener(this), this);
        
        // Register selection tool listener
        selectionToolListener = new SelectionToolListener(this);
        getServer().getPluginManager().registerEvents(selectionToolListener, this);
    }
    
    /**
     * Start the auto-save task
     */
    private void startAutoSaveTask() {
        int interval = configManager.getConfig().getInt("general.auto-save-interval", 300);
        if (interval <= 0) {
            getLogger().info("Auto-save is disabled.");
            return;
        }
        
        autoSaveTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (configManager.isDebugEnabled()) {
                        getLogger().info("Running auto-save...");
                    }
                    dataManager.saveAll();
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Error occurred during auto-save!", e);
                }
            }
        };
        
        // Convert seconds to ticks (20 ticks per second)
        long ticks = interval * 20L;
        autoSaveTask.runTaskTimerAsynchronously(this, ticks, ticks);
        
        getLogger().info("Auto-save enabled with interval: " + interval + " seconds");
    }
    
    /**
     * Initialize advanced features like GUI and performance monitoring
     */
    private void initializeAdvancedFeatures() {
        getLogger().info("Initializing advanced features...");
        
        // Initialize GUI listeners
        guiListener = new GUIListener(this);
        territoryGUIListener = new TerritoryGUIListener(this);
        
        // Register GUI listeners
        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(territoryGUIListener, this);
        
        // Initialize performance monitor
        performanceMonitor = new PerformanceMonitor(this);
        performanceMonitor.start();
        
        getLogger().info("Advanced features initialized successfully!");
    }
    
    /**
     * Reload the plugin configuration and data
     */
    public void reload() {
        getLogger().info("Reloading Countries plugin...");
        
        try {
            // Reload configuration
            configManager.reloadConfigs();
            
            // Reload managers
            countryManager.reload();
            territoryManager.reload();
            economyManager.initialize();
            diplomacyManager.initialize();
            lawSystem.initialize();
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error occurred while reloading plugin!", e);
            throw new RuntimeException("Failed to reload plugin", e);
        }
    }
    
    // Getters for accessing managers and components
    
    public static CountriesPlugin getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }
    
    public CountryManager getCountryManager() {
        return countryManager;
    }
    
    public TerritoryManager getTerritoryManager() {
        return territoryManager;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public DiplomacyManager getDiplomacyManager() {
        return diplomacyManager;
    }
    
    public LawSystem getLawSystem() {
        return lawSystem;
    }
    
    public CountriesAPI getAPI() {
        return api;
    }
    
    public GUIListener getGUIListener() {
        return guiListener;
    }
    
    public TerritoryGUIListener getTerritoryGUIListener() {
        return territoryGUIListener;
    }
    
    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }
    
    public Economy getVaultEconomy() {
        return vaultEconomy;
    }
    
    public boolean hasVaultEconomy() {
        return vaultEconomy != null;
    }
    
    /**
     * Log debug message if debug mode is enabled
     */
    public void debug(String message) {
        if (configManager != null && configManager.isDebugEnabled()) {
            getLogger().info("[DEBUG] " + message);
        }
    }
    
    /**
     * Send formatted message to console
     */
    public void log(String message) {
        getLogger().info(ChatUtils.colorize(configManager.getMessage("general.prefix") + message));
    }
}