package xyz.inv1s1bl3.countries.config;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import xyz.inv1s1bl3.countries.CountriesPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages all configuration files for the Countries plugin.
 * Handles loading, saving, and accessing configuration values.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
public final class ConfigManager {
    
    private final CountriesPlugin plugin;
    private final Map<String, FileConfiguration> configurations;
    
    @Getter
    private FileConfiguration config;
    @Getter
    private FileConfiguration messagesConfig;
    
    public ConfigManager(@NotNull final CountriesPlugin plugin) {
        this.plugin = plugin;
        this.configurations = new HashMap<>();
    }
    
    /**
     * Load all configuration files.
     * 
     * @return true if all configs loaded successfully, false otherwise
     */
    public boolean loadConfigs() {
        try {
            // Load main configuration
            this.config = this.loadConfig("config.yml");
            if (this.config == null) {
                return false;
            }
            
            // Load messages configuration
            final String language = this.config.getString("general.language", "en");
            this.messagesConfig = this.loadConfig("messages.yml");
            if (this.messagesConfig == null) {
                return false;
            }
            
            return true;
            
        } catch (final Exception exception) {
            this.plugin.getLogger().severe("Error loading configurations: " + exception.getMessage());
            exception.printStackTrace();
            return false;
        }
    }
    
    /**
     * Load a specific configuration file.
     * 
     * @param fileName the name of the config file
     * @return the loaded FileConfiguration, or null if failed
     */
    private FileConfiguration loadConfig(@NotNull final String fileName) {
        try {
            final File configFile = new File(this.plugin.getDataFolder(), fileName);
            
            // Create plugin data folder if it doesn't exist
            if (!this.plugin.getDataFolder().exists()) {
                this.plugin.getDataFolder().mkdirs();
            }
            
            // Copy default config from resources if file doesn't exist
            if (!configFile.exists()) {
                try (final InputStream inputStream = this.plugin.getResource(fileName)) {
                    if (inputStream != null) {
                        Files.copy(inputStream, configFile.toPath());
                    } else {
                        this.plugin.getLogger().warning("Default " + fileName + " not found in resources!");
                        return null;
                    }
                }
            }
            
            final FileConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);
            this.configurations.put(fileName, configuration);
            
            this.plugin.getLogger().info("Loaded configuration: " + fileName);
            return configuration;
            
        } catch (final Exception exception) {
            this.plugin.getLogger().severe("Failed to load configuration " + fileName + ": " + exception.getMessage());
            exception.printStackTrace();
            return null;
        }
    }
    
    /**
     * Save a specific configuration file.
     * 
     * @param fileName the name of the config file
     * @return true if saved successfully, false otherwise
     */
    public boolean saveConfig(@NotNull final String fileName) {
        try {
            final FileConfiguration configuration = this.configurations.get(fileName);
            if (configuration == null) {
                this.plugin.getLogger().warning("Configuration " + fileName + " not found!");
                return false;
            }
            
            final File configFile = new File(this.plugin.getDataFolder(), fileName);
            configuration.save(configFile);
            
            return true;
            
        } catch (final IOException exception) {
            this.plugin.getLogger().severe("Failed to save configuration " + fileName + ": " + exception.getMessage());
            exception.printStackTrace();
            return false;
        }
    }
    
    /**
     * Save all configuration files.
     * 
     * @return true if all configs saved successfully, false otherwise
     */
    public boolean saveAllConfigs() {
        boolean success = true;
        
        for (final String fileName : this.configurations.keySet()) {
            if (!this.saveConfig(fileName)) {
                success = false;
            }
        }
        
        return success;
    }
    
    /**
     * Get a message from the messages configuration.
     * 
     * @param path the message path
     * @return the formatted message
     */
    @NotNull
    public String getMessage(@NotNull final String path) {
        if (this.messagesConfig == null) {
            return "Message config not loaded!";
        }
        
        final String message = this.messagesConfig.getString(path);
        if (message == null) {
            this.plugin.getLogger().warning("Message not found: " + path);
            return "Message not found: " + path;
        }
        
        return this.formatMessage(message);
    }
    
    /**
     * Get a message from the messages configuration with placeholders.
     * 
     * @param path the message path
     * @param placeholders the placeholders to replace (key1, value1, key2, value2, ...)
     * @return the formatted message with placeholders replaced
     */
    @NotNull
    public String getMessage(@NotNull final String path, @NotNull final String... placeholders) {
        String message = this.getMessage(path);
        
        // Replace placeholders
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            final String placeholder = placeholders[i];
            final String value = placeholders[i + 1];
            message = message.replace("{" + placeholder + "}", value);
        }
        
        return message;
    }
    
    /**
     * Format a message by applying color codes.
     * 
     * @param message the raw message
     * @return the formatted message
     */
    @NotNull
    private String formatMessage(@NotNull final String message) {
        return message.replace('&', '§');
    }
    
    /**
     * Get a configuration value with a default fallback.
     * 
     * @param path the configuration path
     * @param defaultValue the default value
     * @param <T> the type of the value
     * @return the configuration value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T getConfigValue(@NotNull final String path, @NotNull final T defaultValue) {
        if (this.config == null) {
            return defaultValue;
        }
        
        final Object value = this.config.get(path);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return (T) value;
        } catch (final ClassCastException exception) {
            this.plugin.getLogger().warning("Invalid config value type for " + path + ", using default");
            return defaultValue;
        }
    }
    
    /**
     * Check if debug mode is enabled.
     * 
     * @return true if debug mode is enabled
     */
    public boolean isDebugEnabled() {
        return this.getConfigValue("general.debug", false);
    }
}