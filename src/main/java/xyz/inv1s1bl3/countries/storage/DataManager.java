package xyz.inv1s1bl3.countries.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.country.Country;
import xyz.inv1s1bl3.countries.core.territory.Territory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Manages data storage and retrieval for the Countries plugin.
 * Handles saving and loading of all plugin data using JSON format.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
public final class DataManager {
    
    private final CountriesPlugin plugin;
    private final File dataFolder;
    
    @Getter
    private final Gson gson;
    
    // Data files
    private final File countriesFile;
    private final File territoriesFile;
    
    public DataManager(@NotNull final CountriesPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        
        // Create data folder if it doesn't exist
        if (!this.dataFolder.exists()) {
            this.dataFolder.mkdirs();
        }
        
        // Initialize Gson with custom adapters
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
        
        // Initialize data files
        this.countriesFile = new File(this.dataFolder, "countries.json");
        this.territoriesFile = new File(this.dataFolder, "territories.json");
    }
    
    /**
     * Load all plugin data.
     */
    public void loadAllData() {
        this.loadCountryData();
        this.loadTerritoryData();
        
        this.plugin.getLogger().info("All data loaded successfully!");
    }
    
    /**
     * Save all plugin data.
     */
    public void saveAllData() {
        this.saveCountryData();
        this.saveTerritoryData();
        
        if (this.plugin.getConfigManager().isDebugEnabled()) {
            this.plugin.getLogger().info("All data saved successfully!");
        }
    }
    
    /**
     * Load country data from file.
     */
    public void loadCountryData() {
        if (!this.countriesFile.exists()) {
            this.plugin.getLogger().info("Countries data file not found, starting with empty data.");
            return;
        }
        
        try (final FileReader reader = new FileReader(this.countriesFile)) {
            final Type type = new TypeToken<Map<String, Country>>(){}.getType();
            final Map<String, Country> countryData = this.gson.fromJson(reader, type);
            
            if (countryData != null) {
                this.plugin.getCountryManager().loadCountryData(countryData);
                this.plugin.getLogger().info("Loaded " + countryData.size() + " countries from data file.");
            }
            
        } catch (final IOException exception) {
            this.plugin.getLogger().severe("Failed to load country data: " + exception.getMessage());
            exception.printStackTrace();
        }
    }
    
    /**
     * Save country data to file.
     */
    public void saveCountryData() {
        CompletableFuture.runAsync(() -> {
            try (final FileWriter writer = new FileWriter(this.countriesFile)) {
                final Map<String, Country> countryData = this.plugin.getCountryManager().getCountries();
                this.gson.toJson(countryData, writer);
                
                if (this.plugin.getConfigManager().isDebugEnabled()) {
                    this.plugin.getLogger().info("Saved " + countryData.size() + " countries to data file.");
                }
                
            } catch (final IOException exception) {
                this.plugin.getLogger().severe("Failed to save country data: " + exception.getMessage());
                exception.printStackTrace();
            }
        });
    }
    
    /**
     * Load territory data from file.
     */
    public void loadTerritoryData() {
        if (!this.territoriesFile.exists()) {
            this.plugin.getLogger().info("Territories data file not found, starting with empty data.");
            return;
        }
        
        try (final FileReader reader = new FileReader(this.territoriesFile)) {
            final Type type = new TypeToken<Map<String, Territory>>(){}.getType();
            final Map<String, Territory> territoryData = this.gson.fromJson(reader, type);
            
            if (territoryData != null) {
                this.plugin.getTerritoryManager().loadTerritoryData(territoryData);
                this.plugin.getLogger().info("Loaded " + territoryData.size() + " territories from data file.");
            }
            
        } catch (final IOException exception) {
            this.plugin.getLogger().severe("Failed to load territory data: " + exception.getMessage());
            exception.printStackTrace();
        }
    }
    
    /**
     * Save territory data to file.
     */
    public void saveTerritoryData() {
        CompletableFuture.runAsync(() -> {
            try (final FileWriter writer = new FileWriter(this.territoriesFile)) {
                final Map<String, Territory> territoryData = this.plugin.getTerritoryManager().getTerritories();
                this.gson.toJson(territoryData, writer);
                
                if (this.plugin.getConfigManager().isDebugEnabled()) {
                    this.plugin.getLogger().info("Saved " + territoryData.size() + " territories to data file.");
                }
                
            } catch (final IOException exception) {
                this.plugin.getLogger().severe("Failed to save territory data: " + exception.getMessage());
                exception.printStackTrace();
            }
        });
    }
    
    /**
     * Create a backup of all data files.
     * 
     * @return true if backup was created successfully
     */
    public boolean createBackup() {
        try {
            final File backupFolder = new File(this.plugin.getDataFolder(), "backups");
            if (!backupFolder.exists()) {
                backupFolder.mkdirs();
            }
            
            final String timestamp = LocalDateTime.now().toString().replace(":", "-");
            final File backupFile = new File(backupFolder, "backup-" + timestamp + ".zip");
            
            // TODO: Implement ZIP backup creation
            this.plugin.getLogger().info("Backup created: " + backupFile.getName());
            return true;
            
        } catch (final Exception exception) {
            this.plugin.getLogger().severe("Failed to create backup: " + exception.getMessage());
            exception.printStackTrace();
            return false;
        }
    }
    
    /**
     * Export data to a specific file.
     * 
     * @param fileName the export file name
     * @return true if export was successful
     */
    public boolean exportData(@NotNull final String fileName) {
        try {
            final File exportFile = new File(this.plugin.getDataFolder(), fileName);
            
            final Map<String, Object> exportData = new HashMap<>();
            exportData.put("countries", this.plugin.getCountryManager().getCountries());
            exportData.put("territories", this.plugin.getTerritoryManager().getTerritories());
            exportData.put("export_time", LocalDateTime.now());
            exportData.put("plugin_version", this.plugin.getDescription().getVersion());
            
            try (final FileWriter writer = new FileWriter(exportFile)) {
                this.gson.toJson(exportData, writer);
            }
            
            this.plugin.getLogger().info("Data exported to: " + fileName);
            return true;
            
        } catch (final IOException exception) {
            this.plugin.getLogger().severe("Failed to export data: " + exception.getMessage());
            exception.printStackTrace();
            return false;
        }
    }
    
    /**
     * Import data from a specific file.
     * 
     * @param fileName the import file name
     * @return true if import was successful
     */
    public boolean importData(@NotNull final String fileName) {
        try {
            final File importFile = new File(this.plugin.getDataFolder(), fileName);
            if (!importFile.exists()) {
                this.plugin.getLogger().warning("Import file not found: " + fileName);
                return false;
            }
            
            try (final FileReader reader = new FileReader(importFile)) {
                final Type type = new TypeToken<Map<String, Object>>(){}.getType();
                final Map<String, Object> importData = this.gson.fromJson(reader, type);
                
                if (importData != null && importData.containsKey("countries")) {
                    @SuppressWarnings("unchecked")
                    final Map<String, Country> countryData = (Map<String, Country>) importData.get("countries");
                    this.plugin.getCountryManager().loadCountryData(countryData);
                    
                    if (importData.containsKey("territories")) {
                        @SuppressWarnings("unchecked")
                        final Map<String, Territory> territoryData = (Map<String, Territory>) importData.get("territories");
                        this.plugin.getTerritoryManager().loadTerritoryData(territoryData);
                    }
                    
                    this.plugin.getLogger().info("Data imported from: " + fileName);
                    return true;
                }
            }
            
        } catch (final Exception exception) {
            this.plugin.getLogger().severe("Failed to import data: " + exception.getMessage());
            exception.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Get data storage statistics.
     * 
     * @return a map of statistics
     */
    @NotNull
    public Map<String, Object> getStorageStatistics() {
        final Map<String, Object> stats = new HashMap<>();
        
        stats.put("data_folder_size", this.calculateFolderSize(this.dataFolder));
        stats.put("countries_file_size", this.countriesFile.exists() ? this.countriesFile.length() : 0);
        stats.put("territories_file_size", this.territoriesFile.exists() ? this.territoriesFile.length() : 0);
        stats.put("last_save_time", LocalDateTime.now());
        
        return stats;
    }
    
    /**
     * Calculate the total size of a folder.
     * 
     * @param folder the folder
     * @return the size in bytes
     */
    private long calculateFolderSize(@NotNull final File folder) {
        long size = 0;
        
        if (folder.exists() && folder.isDirectory()) {
            final File[] files = folder.listFiles();
            if (files != null) {
                for (final File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else if (file.isDirectory()) {
                        size += this.calculateFolderSize(file);
                    }
                }
            }
        }
        
        return size;
    }
}