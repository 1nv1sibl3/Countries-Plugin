package xyz.inv1s1bl3.countries.core.country;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages all countries and their operations.
 */
public class CountryManager {
    
    private final CountriesPlugin plugin;
    private final Map<String, Country> countries;
    private final Map<UUID, String> playerCountries; // Player UUID -> Country Name
    
    public CountryManager(CountriesPlugin plugin) {
        this.plugin = plugin;
        this.countries = new ConcurrentHashMap<>();
        this.playerCountries = new ConcurrentHashMap<>();
    }
    
    /**
     * Load all countries from storage
     */
    public void loadCountries() {
        plugin.debug("Loading countries from storage...");
        
        // This will be implemented when storage system is complete
        // For now, we'll just log that loading is ready
        plugin.debug("Country loading system ready");
    }
    
    /**
     * Reload country data
     */
    public void reload() {
        plugin.debug("Reloading country manager...");
        
        // Clear current data
        countries.clear();
        playerCountries.clear();
        
        // Reload from storage
        loadCountries();
        
        plugin.debug("Country manager reloaded successfully");
    }
    
    /**
     * Create a new country
     */
    public boolean createCountry(String name, UUID ownerUUID, String ownerName) {
        // Validate name
        if (!isValidCountryName(name)) {
            return false;
        }
        
        // Check if country already exists
        if (countries.containsKey(name.toLowerCase())) {
            return false;
        }
        
        // Check if player already owns a country
        if (getPlayerCountry(ownerUUID) != null) {
            int maxCountries = plugin.getConfigManager().getConfig()
                    .getInt("general.max-countries-per-player", 1);
            if (maxCountries <= 1) {
                return false;
            }
        }
        
        // Check creation cost
        double creationCost = plugin.getConfigManager().getConfig()
                .getDouble("country.creation-cost", 10000.0);
        
        try {
            // Check and withdraw creation cost BEFORE creating country
            if (plugin.hasVaultEconomy() && creationCost > 0) {
                if (!plugin.getVaultEconomy().has(Bukkit.getOfflinePlayer(ownerUUID), creationCost)) {
                    plugin.debug("Player " + ownerName + " has insufficient funds for country creation");
                    return false;
                }
                
                // Withdraw creation cost
                if (!plugin.getVaultEconomy().withdrawPlayer(Bukkit.getOfflinePlayer(ownerUUID), creationCost).transactionSuccess()) {
                    plugin.debug("Failed to withdraw creation cost from player " + ownerName);
                    return false;
                }
                
                plugin.debug("Withdrew " + creationCost + " from player " + ownerName + " for country creation");
            }
            
            // Create country
            Country country = new Country(name, ownerUUID, GovernmentType.MONARCHY);
            
            // Add owner as citizen
            country.addCitizen(ownerUUID, ownerName, CitizenRole.OWNER);
            
            // Store country
            countries.put(name.toLowerCase(), country);
            playerCountries.put(ownerUUID, name.toLowerCase());
            
            // Save to storage
            saveCountry(country);
            
            plugin.debug("Country '" + name + "' created by " + ownerName);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating country: " + name, e);
            
            // Refund creation cost if something went wrong
            if (plugin.hasVaultEconomy() && creationCost > 0) {
                plugin.getVaultEconomy().depositPlayer(Bukkit.getOfflinePlayer(ownerUUID), creationCost);
            }
            
            return false;
        }
    }
    
    /**
     * Delete a country
     */
    public boolean deleteCountry(String name, UUID requesterUUID) {
        Country country = getCountry(name);
        if (country == null) {
            return false;
        }
        
        // Only owner can delete country
        if (!country.isOwner(requesterUUID)) {
            return false;
        }
        
        try {
            // Remove all citizens from player mapping
            for (Citizen citizen : country.getCitizens()) {
                playerCountries.remove(citizen.getPlayerUUID());
            }
            
            // Remove country
            countries.remove(name.toLowerCase());
            
            // Delete from storage
            deleteCountryFromStorage(country);
            
            plugin.debug("Country '" + name + "' deleted");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error deleting country: " + name, e);
            return false;
        }
    }
    
    /**
     * Get a country by name
     */
    public Country getCountry(String name) {
        if (name == null) return null;
        return countries.get(name.toLowerCase());
    }
    
    /**
     * Get all countries
     */
    public Set<Country> getAllCountries() {
        return new HashSet<>(countries.values());
    }
    
    /**
     * Get country names
     */
    public Set<String> getCountryNames() {
        return countries.values().stream()
                .map(Country::getName)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }
    
    /**
     * Get the country a player is a citizen of
     */
    public Country getPlayerCountry(UUID playerUUID) {
        String countryName = playerCountries.get(playerUUID);
        return countryName != null ? countries.get(countryName) : null;
    }
    
    /**
     * Get the country a player is a citizen of
     */
    public Country getPlayerCountry(Player player) {
        return getPlayerCountry(player.getUniqueId());
    }
    
    /**
     * Check if a player is a citizen of any country
     */
    public boolean isPlayerCitizen(UUID playerUUID) {
        return playerCountries.containsKey(playerUUID);
    }
    
    /**
     * Add a player to a country
     */
    public boolean addPlayerToCountry(UUID playerUUID, String playerName, String countryName) {
        Country country = getCountry(countryName);
        if (country == null || !country.canAddCitizen()) {
            return false;
        }
        
        // Check if player is already in a country
        if (isPlayerCitizen(playerUUID)) {
            return false;
        }
        
        // Add citizen
        if (country.addCitizen(playerUUID, playerName, CitizenRole.CITIZEN)) {
            playerCountries.put(playerUUID, countryName.toLowerCase());
            saveCountry(country);
            return true;
        }
        
        return false;
    }
    
    /**
     * Remove a player from their country
     */
    public boolean removePlayerFromCountry(UUID playerUUID) {
        Country country = getPlayerCountry(playerUUID);
        if (country == null) {
            return false;
        }
        
        // Cannot remove owner
        if (country.isOwner(playerUUID)) {
            return false;
        }
        
        // Remove citizen
        if (country.removeCitizen(playerUUID)) {
            playerCountries.remove(playerUUID);
            saveCountry(country);
            return true;
        }
        
        return false;
    }
    
    /**
     * Invite a player to a country
     */
    public boolean invitePlayer(UUID inviterUUID, UUID inviteeUUID, String countryName) {
        Country country = getCountry(countryName);
        if (country == null) {
            return false;
        }
        
        // Check if inviter has permission
        Citizen inviter = country.getCitizen(inviterUUID);
        if (inviter == null || !inviter.getRole().canInvite()) {
            return false;
        }
        
        // Check if invitee is already in a country
        if (isPlayerCitizen(inviteeUUID)) {
            return false;
        }
        
        // Check if country can accept more citizens
        if (!country.canAddCitizen()) {
            return false;
        }
        
        // Add invitation
        country.addInvitation(inviteeUUID);
        saveCountry(country);
        return true;
    }
    
    /**
     * Accept an invitation to join a country
     */
    public boolean acceptInvitation(UUID playerUUID, String playerName, String countryName) {
        Country country = getCountry(countryName);
        if (country == null || !country.hasInvitation(playerUUID)) {
            return false;
        }
        
        // Add player to country
        if (addPlayerToCountry(playerUUID, playerName, countryName)) {
            country.removeInvitation(playerUUID);
            saveCountry(country);
            return true;
        }
        
        return false;
    }
    
    /**
     * Promote a citizen in a country
     */
    public boolean promoteCitizen(UUID promoterUUID, UUID targetUUID, String countryName) {
        Country country = getCountry(countryName);
        if (country == null) {
            return false;
        }
        
        Citizen promoter = country.getCitizen(promoterUUID);
        Citizen target = country.getCitizen(targetUUID);
        
        if (promoter == null || target == null) {
            return false;
        }
        
        // Check permissions
        if (!promoter.canManage(target)) {
            return false;
        }
        
        // Promote
        if (country.promoteCitizen(targetUUID)) {
            saveCountry(country);
            return true;
        }
        
        return false;
    }
    
    /**
     * Demote a citizen in a country
     */
    public boolean demoteCitizen(UUID demoterUUID, UUID targetUUID, String countryName) {
        Country country = getCountry(countryName);
        if (country == null) {
            return false;
        }
        
        Citizen demoter = country.getCitizen(demoterUUID);
        Citizen target = country.getCitizen(targetUUID);
        
        if (demoter == null || target == null) {
            return false;
        }
        
        // Check permissions
        if (!demoter.canManage(target)) {
            return false;
        }
        
        // Demote
        if (country.demoteCitizen(targetUUID)) {
            saveCountry(country);
            return true;
        }
        
        return false;
    }
    
    /**
     * Kick a citizen from a country
     */
    public boolean kickCitizen(UUID kickerUUID, UUID targetUUID, String countryName) {
        Country country = getCountry(countryName);
        if (country == null) {
            return false;
        }
        
        Citizen kicker = country.getCitizen(kickerUUID);
        Citizen target = country.getCitizen(targetUUID);
        
        if (kicker == null || target == null) {
            return false;
        }
        
        // Check permissions
        if (!kicker.getRole().canKick() || !kicker.canManage(target)) {
            return false;
        }
        
        // Cannot kick owner
        if (country.isOwner(targetUUID)) {
            return false;
        }
        
        // Kick citizen
        if (country.removeCitizen(targetUUID)) {
            playerCountries.remove(targetUUID);
            saveCountry(country);
            return true;
        }
        
        return false;
    }
    
    /**
     * Validate country name
     */
    private boolean isValidCountryName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        int minLength = plugin.getConfigManager().getConfig().getInt("country.min-name-length", 3);
        int maxLength = plugin.getConfigManager().getConfig().getInt("country.max-name-length", 16);
        
        if (name.length() < minLength || name.length() > maxLength) {
            return false;
        }
        
        return ChatUtils.isValidName(name);
    }
    
    /**
     * Save a country to storage
     */
    private void saveCountry(Country country) {
        plugin.getDataManager().executeAsync(() -> {
            try {
                // This will be implemented when storage system is complete
                plugin.debug("Saving country: " + country.getName());
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error saving country: " + country.getName(), e);
            }
        });
    }
    
    /**
     * Delete a country from storage
     */
    private void deleteCountryFromStorage(Country country) {
        plugin.getDataManager().executeAsync(() -> {
            try {
                // This will be implemented when storage system is complete
                plugin.debug("Deleting country from storage: " + country.getName());
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error deleting country from storage: " + country.getName(), e);
            }
        });
    }
    
    /**
     * Get statistics about countries
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total_countries", countries.size());
        stats.put("total_citizens", playerCountries.size());
        stats.put("active_countries", countries.values().stream()
                .mapToInt(country -> country.isActive() ? 1 : 0)
                .sum());
        stats.put("average_citizens_per_country", 
                countries.isEmpty() ? 0 : (double) playerCountries.size() / countries.size());
        
        return stats;
    }
}