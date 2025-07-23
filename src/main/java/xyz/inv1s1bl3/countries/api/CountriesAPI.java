package xyz.inv1s1bl3.countries.api;

import org.bukkit.entity.Player;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.country.Country;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Public API for the Countries plugin.
 * Provides safe access to plugin functionality for other plugins.
 */
public class CountriesAPI {
    
    private final CountriesPlugin plugin;
    
    public CountriesAPI(CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Get a country by name
     */
    public Optional<Country> getCountry(String name) {
        return Optional.ofNullable(plugin.getCountryManager().getCountry(name));
    }
    
    /**
     * Get a country by player UUID (the country they're a citizen of)
     */
    public Optional<Country> getPlayerCountry(UUID playerUUID) {
        return Optional.ofNullable(plugin.getCountryManager().getPlayerCountry(playerUUID));
    }
    
    /**
     * Get a country by player
     */
    public Optional<Country> getPlayerCountry(Player player) {
        return getPlayerCountry(player.getUniqueId());
    }
    
    /**
     * Get all countries
     */
    public Set<Country> getAllCountries() {
        return plugin.getCountryManager().getAllCountries();
    }
    
    /**
     * Check if a player is a citizen of any country
     */
    public boolean isPlayerCitizen(UUID playerUUID) {
        return getPlayerCountry(playerUUID).isPresent();
    }
    
    /**
     * Check if a player is a citizen of any country
     */
    public boolean isPlayerCitizen(Player player) {
        return isPlayerCitizen(player.getUniqueId());
    }
    
    /**
     * Get player's balance (if using Countries economy)
     */
    public double getPlayerBalance(UUID playerUUID) {
        // This will be implemented when economy system is added
        return 0.0;
    }
    
    /**
     * Get player's balance (if using Countries economy)
     */
    public double getPlayerBalance(Player player) {
        return getPlayerBalance(player.getUniqueId());
    }
    
    /**
     * Get country's balance
     */
    public double getCountryBalance(String countryName) {
        Country country = plugin.getCountryManager().getCountry(countryName);
        return country != null ? country.getBalance() : 0.0;
    }
    
    /**
     * Check if plugin is properly loaded and functional
     */
    public boolean isPluginReady() {
        return plugin.isEnabled() && 
               plugin.getCountryManager() != null;
    }
    
    /**
     * Get plugin version
     */
    public String getPluginVersion() {
        return plugin.getDescription().getVersion();
    }
}