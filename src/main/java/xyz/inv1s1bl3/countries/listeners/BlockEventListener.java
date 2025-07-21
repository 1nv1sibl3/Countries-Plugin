package xyz.inv1s1bl3.countries.listeners;

import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.utils.MessageUtil;

/**
 * Handles block-related events
 */
@RequiredArgsConstructor
public final class BlockEventListener implements Listener {
    
    private final CountriesPlugin plugin;
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        if (!this.plugin.getTerritoryManager().canPlayerBreak(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            
            final String countryName = this.plugin.getTerritoryManager().getTerritoryAt(event.getBlock().getLocation())
                .flatMap(territory -> this.plugin.getCountryManager().getCountry(territory.getCountryId()))
                .map(xyz.inv1s1bl3.countries.database.entities.Country::getName)
                .orElse("Unknown");
            
            MessageUtil.sendMessage(event.getPlayer(), "territory.protection-block-break", 
                java.util.Map.of("country", countryName));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        if (!this.plugin.getTerritoryManager().canPlayerBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            
            final String countryName = this.plugin.getTerritoryManager().getTerritoryAt(event.getBlock().getLocation())
                .flatMap(territory -> this.plugin.getCountryManager().getCountry(territory.getCountryId()))
                .map(xyz.inv1s1bl3.countries.database.entities.Country::getName)
                .orElse("Unknown");
            
            MessageUtil.sendMessage(event.getPlayer(), "territory.protection-block-place", 
                java.util.Map.of("country", countryName));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getClickedBlock() != null && 
            !this.plugin.getTerritoryManager().getProtectionManager().canInteract(
                event.getPlayer(), event.getClickedBlock(), event.getAction())) {
            event.setCancelled(true);
            MessageUtil.sendMessage(event.getPlayer(), "territory.protection-interaction");
        }
    }
}