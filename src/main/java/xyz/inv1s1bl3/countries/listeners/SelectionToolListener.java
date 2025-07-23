package xyz.inv1s1bl3.countries.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

/**
 * Handles selection tool interactions for territory management.
 */
public class SelectionToolListener implements Listener {
    
    private final CountriesPlugin plugin;
    
    public SelectionToolListener(CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        var player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || item.getType() != Material.GOLDEN_AXE) {
            return;
        }
        
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }
        
        String displayName = ChatUtils.stripColors(item.getItemMeta().getDisplayName());
        if (!"Territory Selection Tool".equals(displayName)) {
            return;
        }
        
        if (event.getClickedBlock() == null) {
            return;
        }
        
        event.setCancelled(true);
        
        var location = event.getClickedBlock().getLocation();
        
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Set corner 1
            plugin.getTerritoryManager().setSelectionCorner(player, location, true);
            ChatUtils.sendSuccess(player, "Corner 1 set at " + 
                    location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
            
            // Show selection visualization
            var corners = plugin.getTerritoryManager().getPlayerSelection(player);
            if (corners[1] != null) {
                plugin.getTerritoryManager().getBorderVisualizer().showSelection(player, location, corners[1]);
            }
            
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Set corner 2
            plugin.getTerritoryManager().setSelectionCorner(player, location, false);
            ChatUtils.sendSuccess(player, "Corner 2 set at " + 
                    location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
            
            // Show selection visualization
            var corners = plugin.getTerritoryManager().getPlayerSelection(player);
            if (corners[0] != null) {
                plugin.getTerritoryManager().getBorderVisualizer().showSelection(player, corners[0], location);
            }
        }
    }
}