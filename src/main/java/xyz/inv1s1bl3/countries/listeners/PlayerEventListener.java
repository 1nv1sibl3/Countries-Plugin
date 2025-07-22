package xyz.inv1s1bl3.countries.listeners;

import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xyz.inv1s1bl3.countries.CountriesPlugin;

/**
 * Handles player-related events
 */
@RequiredArgsConstructor
public final class PlayerEventListener implements Listener {
    
    private final CountriesPlugin plugin;
    
    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        // Get or create player data
        this.plugin.getCountryManager().getOrCreatePlayer(
            event.getPlayer().getUniqueId(),
            event.getPlayer().getName()
        );
        
        // Set online status
        this.plugin.getCountryManager().setPlayerOnlineStatus(
            event.getPlayer().getUniqueId(),
            true
        );
        
        // Handle economy integration
        this.plugin.getEconomyManager().handlePlayerJoin(event.getPlayer().getUniqueId());
    }
    
    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        // Set offline status
        this.plugin.getCountryManager().setPlayerOnlineStatus(
            event.getPlayer().getUniqueId(),
            false
        );
    }
    
    @EventHandler
    public void onPlayerMove(final PlayerMoveEvent event) {
        // Only process if player moved to a different chunk
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }
        
        this.plugin.getTerritoryManager().handlePlayerMovement(event.getPlayer(), event.getTo());
    }
}