package xyz.inv1s1bl3.countries.utils;

import org.bukkit.entity.Player;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.country.GovernmentType;
import xyz.inv1s1bl3.countries.database.entities.Country;

import java.util.Optional;

/**
 * Utility class for permission checking
 */
public final class PermissionUtil {
    
    private PermissionUtil() {
        // Utility class
    }
    
    /**
     * Check if player has a specific permission
     * @param player Player to check
     * @param permission Permission to check
     * @return true if player has permission
     */
    public static boolean hasPermission(final Player player, final String permission) {
        return player.hasPermission(permission);
    }
    
    /**
     * Check if player has bypass permissions
     * @param player Player to check
     * @return true if player can bypass restrictions
     */
    public static boolean hasBypassPermission(final Player player) {
        return player.hasPermission("countries.bypass") || player.isOp();
    }
    
    /**
     * Check if player has admin permissions
     * @param player Player to check
     * @return true if player has admin permissions
     */
    public static boolean hasAdminPermission(final Player player) {
        return player.hasPermission("countries.admin") || player.isOp();
    }
    
    /**
     * Check if player can create countries
     * @param player Player to check
     * @return true if player can create countries
     */
    public static boolean canCreateCountry(final Player player) {
        return player.hasPermission("countries.country.create");
    }
    
    /**
     * Check if player can dissolve countries
     * @param player Player to check
     * @return true if player can dissolve countries
     */
    public static boolean canDissolveCountry(final Player player) {
        return player.hasPermission("countries.country.dissolve");
    }
    
    /**
     * Check if player can claim territory
     * @param player Player to check
     * @return true if player can claim territory
     */
    public static boolean canClaimTerritory(final Player player) {
        return player.hasPermission("countries.territory.claim");
    }
    
    /**
     * Check if player can unclaim territory
     * @param player Player to check
     * @return true if player can unclaim territory
     */
    public static boolean canUnclaimTerritory(final Player player) {
        return player.hasPermission("countries.territory.unclaim");
    }
    
    /**
     * Check if player can manage diplomacy
     * @param player Player to check
     * @return true if player can manage diplomacy
     */
    public static boolean canManageDiplomacy(final Player player) {
        return player.hasPermission("countries.diplomacy.manage");
    }
    
    /**
     * Check if player can transfer money
     * @param player Player to check
     * @return true if player can transfer money
     */
    public static boolean canTransferMoney(final Player player) {
        return player.hasPermission("countries.economy.transfer");
    }
    
    /**
     * Check if player can initiate trades
     * @param player Player to check
     * @return true if player can initiate trades
     */
    public static boolean canInitiateTrade(final Player player) {
        return player.hasPermission("countries.trade.initiate");
    }
    
    /**
     * Check if player has country leadership permissions
     * @param player Bukkit player
     * @param countryId Country ID to check
     * @return true if player is leader of the country
     */
    public static boolean isCountryLeader(final Player player, final Integer countryId) {
        final CountriesPlugin plugin = CountriesPlugin.getInstance();
        final Optional<xyz.inv1s1bl3.countries.database.entities.Player> playerDataOpt = 
                plugin.getCountryManager().getPlayer(player.getUniqueId());
        
        if (playerDataOpt.isEmpty()) {
            return false;
        }
        
        final xyz.inv1s1bl3.countries.database.entities.Player playerData = playerDataOpt.get();
        
        // Check if player is in the country
        if (!playerData.getCountryId().equals(countryId)) {
            return false;
        }
        
        // Check if player is a leader
        return playerData.isLeader();
    }
    
    /**
     * Check if player has administrative permissions in their country
     * @param player Bukkit player
     * @return true if player has admin permissions in their country
     */
    public static boolean hasCountryAdminPermissions(final Player player) {
        final CountriesPlugin plugin = CountriesPlugin.getInstance();
        final Optional<xyz.inv1s1bl3.countries.database.entities.Player> playerDataOpt = 
                plugin.getCountryManager().getPlayer(player.getUniqueId());
        
        if (playerDataOpt.isEmpty()) {
            return false;
        }
        
        final xyz.inv1s1bl3.countries.database.entities.Player playerData = playerDataOpt.get();
        return playerData.hasAdminPermissions();
    }
    
    /**
     * Check if player can perform action based on government hierarchy
     * @param actingPlayer Player performing the action
     * @param targetPlayer Target player (can be null for non-player actions)
     * @param requiredAuthority Required authority level
     * @return true if player has sufficient authority
     */
    public static boolean hasGovernmentAuthority(final Player actingPlayer, final Player targetPlayer, final String requiredAuthority) {
        final CountriesPlugin plugin = CountriesPlugin.getInstance();
        
        // Get acting player data
        final Optional<xyz.inv1s1bl3.countries.database.entities.Player> actingPlayerDataOpt = 
                plugin.getCountryManager().getPlayer(actingPlayer.getUniqueId());
        
        if (actingPlayerDataOpt.isEmpty()) {
            return false;
        }
        
        final xyz.inv1s1bl3.countries.database.entities.Player actingPlayerData = actingPlayerDataOpt.get();
        
        // Check if player is in a country
        if (!actingPlayerData.hasCountry()) {
            return false;
        }
        
        // Get country information
        final Optional<Country> countryOpt = plugin.getCountryManager().getCountry(actingPlayerData.getCountryId());
        if (countryOpt.isEmpty()) {
            return false;
        }
        
        final Country country = countryOpt.get();
        final Optional<GovernmentType> govTypeOpt = GovernmentType.fromKey(country.getGovernmentType());
        if (govTypeOpt.isEmpty()) {
            return false;
        }
        
        final GovernmentType govType = govTypeOpt.get();
        
        // Check if acting player has required authority
        if (!govType.hasRole(actingPlayerData.getRole()) || !govType.hasRole(requiredAuthority)) {
            return false;
        }
        
        final boolean hasRequiredAuthority = govType.hasHigherAuthority(actingPlayerData.getRole(), requiredAuthority) || 
                                           actingPlayerData.getRole().equals(requiredAuthority);
        
        if (!hasRequiredAuthority) {
            return false;
        }
        
        // If there's a target player, check hierarchy
        if (targetPlayer != null) {
            final Optional<xyz.inv1s1bl3.countries.database.entities.Player> targetPlayerDataOpt = 
                    plugin.getCountryManager().getPlayer(targetPlayer.getUniqueId());
            
            if (targetPlayerDataOpt.isPresent()) {
                final xyz.inv1s1bl3.countries.database.entities.Player targetPlayerData = targetPlayerDataOpt.get();
                
                // Check if both players are in the same country
                if (targetPlayerData.hasCountry() && targetPlayerData.getCountryId().equals(actingPlayerData.getCountryId())) {
                    // Acting player must have higher authority than target player
                    return govType.hasHigherAuthority(actingPlayerData.getRole(), targetPlayerData.getRole());
                }
            }
        }
        
        return true;
    }
    
    /**
     * Get player's authority level in their country
     * @param player Player to check
     * @return Authority level (0 = highest), or -1 if not in country
     */
    public static int getPlayerAuthorityLevel(final Player player) {
        final CountriesPlugin plugin = CountriesPlugin.getInstance();
        final Optional<xyz.inv1s1bl3.countries.database.entities.Player> playerDataOpt = 
                plugin.getCountryManager().getPlayer(player.getUniqueId());
        
        if (playerDataOpt.isEmpty()) {
            return -1;
        }
        
        final xyz.inv1s1bl3.countries.database.entities.Player playerData = playerDataOpt.get();
        
        if (!playerData.hasCountry()) {
            return -1;
        }
        
        final Optional<Country> countryOpt = plugin.getCountryManager().getCountry(playerData.getCountryId());
        if (countryOpt.isEmpty()) {
            return -1;
        }
        
        final Country country = countryOpt.get();
        final Optional<GovernmentType> govTypeOpt = GovernmentType.fromKey(country.getGovernmentType());
        if (govTypeOpt.isEmpty()) {
            return -1;
        }
        
        final GovernmentType govType = govTypeOpt.get();
        return govType.getRoleLevel(playerData.getRole());
    }
}