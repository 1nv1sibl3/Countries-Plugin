package xyz.inv1s1bl3.countries.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.inv1s1bl3.countries.CountriesPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Manages configuration files for the Countries plugin.
 * Handles loading, saving, and accessing configuration values.
 */
public class ConfigManager {
    
    private final CountriesPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    
    private File configFile;
    private File messagesFile;
    
    private boolean debugMode;
    
    public ConfigManager(CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load all configuration files
     */
    public void loadConfigs() {
        // Create plugin data folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        // Load main config
        loadConfig();
        
        // Load messages config
        loadMessages();
        
        // Set debug mode
        debugMode = config.getBoolean("general.enable-debug", false);
        
        plugin.getLogger().info("Configuration loaded successfully!");
    }
    
    /**
     * Reload all configuration files
     */
    public void reloadConfigs() {
        loadConfigs();
        plugin.getLogger().info("Configuration reloaded successfully!");
    }
    
    /**
     * Load main configuration file
     */
    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Validate configuration
        validateConfig();
    }
    
    /**
     * Load messages configuration file
     */
    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Validate messages
        validateMessages();
    }
    
    /**
     * Validate main configuration values
     */
    private void validateConfig() {
        boolean changed = false;
        
        // Validate auto-save interval
        if (config.getInt("general.auto-save-interval") < 0) {
            config.set("general.auto-save-interval", 300);
            changed = true;
        }
        
        // Validate max countries per player
        if (config.getInt("general.max-countries-per-player") < 1) {
            config.set("general.max-countries-per-player", 1);
            changed = true;
        }
        
        // Validate economy settings
        if (config.getDouble("economy.starting-balance") < 0) {
            config.set("economy.starting-balance", 1000.0);
            changed = true;
        }
        
        if (config.getDouble("economy.default-tax-rate") < 0 || config.getDouble("economy.default-tax-rate") > 1) {
            config.set("economy.default-tax-rate", 0.05);
            changed = true;
        }
        
        if (config.getDouble("economy.max-tax-rate") < 0 || config.getDouble("economy.max-tax-rate") > 1) {
            config.set("economy.max-tax-rate", 0.25);
            changed = true;
        }
        
        // Validate territory settings
        if (config.getDouble("territory.chunk-claim-cost") < 0) {
            config.set("territory.chunk-claim-cost", 100.0);
            changed = true;
        }
        
        if (config.getInt("territory.max-chunks-per-territory") < 1) {
            config.set("territory.max-chunks-per-territory", 100);
            changed = true;
        }
        
        // Validate country settings
        if (config.getInt("country.min-name-length") < 1) {
            config.set("country.min-name-length", 3);
            changed = true;
        }
        
        if (config.getInt("country.max-name-length") < config.getInt("country.min-name-length")) {
            config.set("country.max-name-length", 16);
            changed = true;
        }
        
        if (config.getDouble("country.creation-cost") < 0) {
            config.set("country.creation-cost", 10000.0);
            changed = true;
        }
        
        if (changed) {
            saveConfig();
            plugin.getLogger().info("Configuration values were corrected and saved.");
        }
    }
    
    /**
     * Validate messages configuration
     */
    private void validateMessages() {
        boolean changed = false;
        
        // Ensure essential messages exist
        String[] essentialKeys = {
            "general.prefix",
            "general.no-permission",
            "general.player-not-found",
            "country.created",
            "country.not-found",
            "territory.claimed",
            "territory.protected",
            "economy.insufficient-funds",
            "diplomacy.alliance-proposed"
        };
        
        for (String key : essentialKeys) {
            if (!messages.contains(key)) {
                plugin.getLogger().warning("Missing message key: " + key);
                // You could set default values here if needed
            }
        }
        
        if (changed) {
            saveMessages();
        }
    }
    
    /**
     * Save main configuration file
     */
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config.yml!", e);
        }
    }
    
    /**
     * Save messages configuration file
     */
    public void saveMessages() {
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save messages.yml!", e);
        }
    }
    
    /**
     * Get main configuration
     */
    public FileConfiguration getConfig() {
        return config;
    }
    
    /**
     * Get messages configuration
     */
    public FileConfiguration getMessages() {
        return messages;
    }
    
    /**
     * Get a message from the messages configuration
     */
    public String getMessage(String key) {
        return messages.getString(key, "&cMessage not found: " + key);
    }
    
    /**
     * Get a message with placeholders replaced
     */
    public String getMessage(String key, Object... placeholders) {
        String message = getMessage(key);
        
        for (int i = 0; i < placeholders.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(placeholders[i]));
        }
        
        return message;
    }
    
    /**
     * Check if debug mode is enabled
     */
    public boolean isDebugEnabled() {
        return debugMode;
    }
    
    /**
     * Toggle debug mode
     */
    public void toggleDebug() {
        debugMode = !debugMode;
        config.set("general.enable-debug", debugMode);
        saveConfig();
    }
    
    /**
     * Get configuration value with default
     */
    public <T> T getConfigValue(String path, T defaultValue) {
        if (!config.contains(path)) {
            return defaultValue;
        }
        
        Object value = config.get(path);
        try {
            return (T) value;
        } catch (ClassCastException e) {
            plugin.getLogger().warning("Invalid configuration value for " + path + ": " + value);
            return defaultValue;
        }
    }
}