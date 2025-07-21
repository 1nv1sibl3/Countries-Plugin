package xyz.inv1s1bl3.countries.config;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.inv1s1bl3.countries.CountriesPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Manages all configuration files for the Countries plugin
 */
@Getter
public final class ConfigManager {
    
    private final CountriesPlugin plugin;
    
    // Configuration files
    private FileConfiguration mainConfig;
    private FileConfiguration messageConfig;
    private FileConfiguration territoryConfig;
    private FileConfiguration governmentConfig;
    
    // Configuration file objects
    private File mainConfigFile;
    private File messageConfigFile;
    private File territoryConfigFile;
    private File governmentConfigFile;
    
    public ConfigManager(final CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load all configuration files
     */
    public void loadConfigurations() {
        // Create plugin data folder if it doesn't exist
        if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
        }
        
        this.loadMainConfig();
        this.loadMessageConfig();
        this.loadTerritoryConfig();
        this.loadGovernmentConfig();
        
        this.plugin.getLogger().info("All configuration files loaded successfully!");
    }
    
    /**
     * Load the main configuration file
     */
    private void loadMainConfig() {
        this.mainConfigFile = new File(this.plugin.getDataFolder(), "config.yml");
        
        if (!this.mainConfigFile.exists()) {
            this.plugin.saveResource("config.yml", false);
        }
        
        this.mainConfig = YamlConfiguration.loadConfiguration(this.mainConfigFile);
        this.plugin.getLogger().info("Main configuration loaded!");
    }
    
    /**
     * Load the message configuration file
     */
    private void loadMessageConfig() {
        this.messageConfigFile = new File(this.plugin.getDataFolder(), "messages.yml");
        
        if (!this.messageConfigFile.exists()) {
            this.plugin.saveResource("messages.yml", false);
        }
        
        this.messageConfig = YamlConfiguration.loadConfiguration(this.messageConfigFile);
        this.plugin.getLogger().info("Message configuration loaded!");
    }
    
    /**
     * Load the territory configuration file
     */
    private void loadTerritoryConfig() {
        this.territoryConfigFile = new File(this.plugin.getDataFolder(), "territories.yml");
        
        if (!this.territoryConfigFile.exists()) {
            this.plugin.saveResource("territories.yml", false);
        }
        
        this.territoryConfig = YamlConfiguration.loadConfiguration(this.territoryConfigFile);
        this.plugin.getLogger().info("Territory configuration loaded!");
    }
    
    /**
     * Load the government configuration file
     */
    private void loadGovernmentConfig() {
        this.governmentConfigFile = new File(this.plugin.getDataFolder(), "governments.yml");
        
        if (!this.governmentConfigFile.exists()) {
            this.plugin.saveResource("governments.yml", false);
        }
        
        this.governmentConfig = YamlConfiguration.loadConfiguration(this.governmentConfigFile);
        this.plugin.getLogger().info("Government configuration loaded!");
    }
    
    /**
     * Save a configuration file
     */
    public void saveConfig(final String configName) {
        try {
            switch (configName.toLowerCase()) {
                case "main" -> this.mainConfig.save(this.mainConfigFile);
                case "messages" -> this.messageConfig.save(this.messageConfigFile);
                case "territories" -> this.territoryConfig.save(this.territoryConfigFile);
                case "governments" -> this.governmentConfig.save(this.governmentConfigFile);
                default -> this.plugin.getLogger().warning("Unknown config file: " + configName);
            }
        } catch (final IOException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Could not save " + configName + " config file!", exception);
        }
    }
    
    /**
     * Reload all configurations
     */
    public void reloadConfigurations() {
        this.loadConfigurations();
        this.plugin.getLogger().info("All configurations reloaded!");
    }
    
    // Convenience methods for common config values
    
    public boolean isDebugEnabled() {
        return this.mainConfig.getBoolean("general.debug", false);
    }
    
    public String getLanguage() {
        return this.mainConfig.getString("general.language", "en");
    }
    
    public int getSaveInterval() {
        return this.mainConfig.getInt("general.save-interval", 5);
    }
    
    public boolean isDatabaseEnabled() {
        return this.mainConfig.getBoolean("database.enabled", true);
    }
    
    public String getDatabaseType() {
        return this.mainConfig.getString("database.type", "sqlite");
    }
    
    public String getDatabaseFile() {
        return this.mainConfig.getString("database.file", "countries.db");
    }
    
    public boolean areCountriesEnabled() {
        return this.mainConfig.getBoolean("countries.enabled", true);
    }
    
    public int getMaxCountriesPerPlayer() {
        return this.mainConfig.getInt("countries.max-countries-per-player", 1);
    }
    
    public double getCountryCreationCost() {
        return this.mainConfig.getDouble("countries.creation-cost", 1000.0);
    }
    
    public boolean areTerritoriesEnabled() {
        return this.mainConfig.getBoolean("territories.enabled", true);
    }
    
    public int getMaxChunksPerCountry() {
        return this.mainConfig.getInt("territories.max-chunks-per-country", 100);
    }
    
    public double getBaseChunkPrice() {
        return this.mainConfig.getDouble("territories.base-chunk-price", 100.0);
    }
    
    public boolean isEconomyEnabled() {
        return this.mainConfig.getBoolean("economy.enabled", true);
    }
    
    public boolean shouldUseVault() {
        return this.mainConfig.getBoolean("economy.use-vault", true);
    }
    
    public double getStartingBalance() {
        return this.mainConfig.getDouble("economy.starting-balance", 1000.0);
    }
    
    public boolean isTradingEnabled() {
        return this.mainConfig.getBoolean("trading.enabled", true);
    }
    
    public boolean isDiplomacyEnabled() {
        return this.mainConfig.getBoolean("diplomacy.enabled", true);
    }
    
    public boolean isLegalSystemEnabled() {
        return this.mainConfig.getBoolean("legal.enabled", true);
    }
    
    public boolean isChatEnabled() {
        return this.mainConfig.getBoolean("chat.enable-country-chat", true);
    }
    
    public boolean areGUIsEnabled() {
        return this.mainConfig.getBoolean("gui.enabled", true);
    }
    
    public boolean areVisualizationsEnabled() {
        return this.mainConfig.getBoolean("visualization.enabled", true);
    }
}