package xyz.inv1s1bl3.countries.listeners;

import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.entity.Player;
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
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (!this.plugin.getTerritoryManager().getAdvancedProtectionManager()
                .canDamageEntity(player, event.getEntity())) {
                event.setCancelled(true);
                MessageUtil.sendMessage(player, "territory.protection-entity-damage");
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(final CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
            if (!this.plugin.getTerritoryManager().isMobSpawningAllowed(
                event.getLocation(), event.getEntityType())) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(final PlayerMoveEvent event) {
        // Only check if player moved to different chunk
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }
        
        // Check if player can enter the new territory
        if (!this.plugin.getTerritoryManager().getAdvancedProtectionManager()
            .canEnterTerritory(event.getPlayer(), event.getTo())) {
            event.setCancelled(true);
            MessageUtil.sendMessage(event.getPlayer(), "territory.protection-entry-denied");
        }
    }
}