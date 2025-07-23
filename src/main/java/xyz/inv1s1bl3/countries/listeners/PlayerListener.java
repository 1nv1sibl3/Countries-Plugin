package xyz.inv1s1bl3.countries.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.country.Country;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

/**
 * Handles player-related events for the Countries plugin.
 */
public class PlayerListener implements Listener {
    
    private final CountriesPlugin plugin;
    
    public PlayerListener(CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        
        // Check if player has a country
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country != null) {
            // Welcome back message
            ChatUtils.sendPrefixedMessage(player, 
                    "&aWelcome back to &e" + country.getName() + "&a!");
            
            // Check for pending invitations or notifications
            if (!country.getInvitations().isEmpty() && 
                country.getCitizen(player.getUniqueId()).getRole().canInvite()) {
                ChatUtils.sendInfo(player, 
                        "Your country has " + country.getInvitations().size() + " pending invitations.");
            }
        }
        
        plugin.debug("Player " + player.getName() + " joined - Country: " + 
                    (country != null ? country.getName() : "None"));
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();
        
        plugin.debug("Player " + player.getName() + " left the server");
    }
}