package xyz.inv1s1bl3.countries.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.country.Country;

/**
 * Handles player-related events for the Countries plugin.
 * Manages player data and country interactions.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
public final class PlayerListener implements Listener {
    
    private final CountriesPlugin plugin;
    
    public PlayerListener(@NotNull final CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle player join events.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(@NotNull final PlayerJoinEvent event) {
        final Country country = this.plugin.getCountryManager().getPlayerCountry(event.getPlayer().getUniqueId());
        
        if (country != null) {
            // Update country activity
            country.updateActivity();
            
            // TODO: Send welcome message with country info
            // TODO: Check for pending invitations
            // TODO: Update player data
        }
    }
    
    /**
     * Handle player quit events.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull final PlayerQuitEvent event) {
        final Country country = this.plugin.getCountryManager().getPlayerCountry(event.getPlayer().getUniqueId());
        
        if (country != null) {
            // Update country activity
            country.updateActivity();
            
            // TODO: Save player data
            // TODO: Update last seen timestamp
        }
    }
}