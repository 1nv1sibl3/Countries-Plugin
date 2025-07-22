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
        
        // Check for territory entry notifications
        this.handleTerritoryEntryNotification(event.getPlayer(), event.getTo());
    }
    
    /**
     * Handle territory entry notifications
     * @param player Player entering territory
     * @param location Location entered
     */
    private void handleTerritoryEntryNotification(final org.bukkit.entity.Player player, final org.bukkit.Location location) {
        final var territoryOpt = this.plugin.getTerritoryManager().getTerritoryAt(location);
        
        if (territoryOpt.isPresent()) {
            final var territory = territoryOpt.get();
            final var countryOpt = this.plugin.getCountryManager().getCountry(territory.getCountryId());
            
            if (countryOpt.isPresent()) {
                final var country = countryOpt.get();
                
                // Check if player is entering their own country's territory
                final var playerDataOpt = this.plugin.getCountryManager().getPlayer(player.getUniqueId());
                
                if (playerDataOpt.isPresent() && playerDataOpt.get().hasCountry() && 
                    playerDataOpt.get().getCountryId().equals(territory.getCountryId())) {
                    // Entering own territory
                    xyz.inv1s1bl3.countries.utils.MessageUtil.sendInfo(player, 
                        "Entering " + country.getName() + " territory (" + territory.getTerritoryType() + ")");
                } else {
                    // Entering foreign territory
                    xyz.inv1s1bl3.countries.utils.MessageUtil.sendWarning(player, 
                        "Entering " + country.getName() + " territory - respect local laws!");
                }
            }
        }
    }
}