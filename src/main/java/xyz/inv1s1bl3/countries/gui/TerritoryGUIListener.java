package xyz.inv1s1bl3.countries.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.territory.*;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles GUI click events for territory management.
 */
public class TerritoryGUIListener implements Listener {
    
    private final CountriesPlugin plugin;
    private final TerritoryGUI territoryGUI;
    
    public TerritoryGUIListener(CountriesPlugin plugin) {
        this.plugin = plugin;
        this.territoryGUI = new TerritoryGUI(plugin);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        if (event.getInventory().getType() == InventoryType.PLAYER) {
            return;
        }
        
        String title = ChatUtils.stripColors(event.getView().getTitle());
        
        // Handle different GUI types
        if (title.contains("Territory Management")) {
            handleTerritoryManagementClick(event, player);
        } else if (title.contains("Territories -")) {
            handleTerritoryListClick(event, player);
        } else if (title.contains("Flags -")) {
            handleFlagsClick(event, player);
        } else if (title.contains("Roles -")) {
            handleRolesClick(event, player);
        } else if (title.contains("Sub-Areas -")) {
            handleSubAreasClick(event, player);
        } else if (title.contains("Rental Management -")) {
            handleRentalClick(event, player);
        } else if (title.contains("Claiming Tools")) {
            handleClaimingToolsClick(event, player);
        }
    }
    
    private void handleTerritoryManagementClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        
        String itemName = ChatUtils.stripColors(clicked.getItemMeta().getDisplayName());
        
        switch (itemName.toLowerCase()) {
            case "your territories" -> {
                territoryGUI.openTerritoryList(player, 0);
            }
            case "claiming tool" -> {
                territoryGUI.openClaimingGUI(player);
            }
            case "current territory" -> {
                Territory territory = plugin.getTerritoryManager().getTerritoryAt(player.getLocation());
                if (territory != null) {
                    openTerritoryDetails(player, territory);
                }
            }
            case "wilderness" -> {
                player.closeInventory();
                ChatUtils.sendInfo(player, "Use /territory claim <name> to claim this area!");
            }
            case "claiming limits" -> {
                showClaimingLimits(player);
            }
            case "border visualization" -> {
                Territory territory = plugin.getTerritoryManager().getTerritoryAt(player.getLocation());
                if (territory != null) {
                    plugin.getTerritoryManager().getBorderVisualizer().showBorders(player, territory);
                    ChatUtils.sendInfo(player, "Showing borders for " + territory.getName());
                } else {
                    ChatUtils.sendError(player, "No territory at your current location!");
                }
                player.closeInventory();
            }
            case "flags & permissions" -> {
                Territory territory = plugin.getTerritoryManager().getTerritoryAt(player.getLocation());
                if (territory != null) {
                    territoryGUI.openFlagsGUI(player, territory);
                } else {
                    ChatUtils.sendError(player, "No territory at your current location!");
                }
            }
            case "sub-areas" -> {
                Territory territory = plugin.getTerritoryManager().getTerritoryAt(player.getLocation());
                if (territory != null) {
                    territoryGUI.openSubAreasGUI(player, territory);
                } else {
                    ChatUtils.sendError(player, "No territory at your current location!");
                }
            }
            case "rental system" -> {
                Territory territory = plugin.getTerritoryManager().getTerritoryAt(player.getLocation());
                if (territory != null) {
                    territoryGUI.openRentalGUI(player, territory);
                } else {
                    ChatUtils.sendError(player, "No territory at your current location!");
                }
            }
            case "close" -> {
                player.closeInventory();
            }
        }
    }
    
    private void handleTerritoryListClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        
        String itemName = ChatUtils.stripColors(clicked.getItemMeta().getDisplayName());
        
        if ("Previous Page".equals(itemName)) {
            // Handle previous page
        } else if ("Next Page".equals(itemName)) {
            // Handle next page
        } else if ("Create Territory".equals(itemName)) {
            player.closeInventory();
            ChatUtils.sendInfo(player, "Use /territory claim <name> to create a new territory!");
        } else {
            // Territory item clicked
            Territory territory = plugin.getTerritoryManager().getTerritory(itemName);
            if (territory != null) {
                if (event.isLeftClick()) {
                    openTerritoryDetails(player, territory);
                } else if (event.isRightClick()) {
                    // Teleport to territory center
                    ChunkCoordinate center = territory.getCenterChunk();
                    if (center != null) {
                        player.teleport(center.getChunk(player.getWorld()).getBlock(8, player.getWorld().getHighestBlockYAt(center.getX() * 16 + 8, center.getZ() * 16 + 8) + 1, 8).getLocation());
                        ChatUtils.sendInfo(player, "Teleported to " + territory.getName());
                        player.closeInventory();
                    }
                }
            }
        }
    }
    
    private void handleFlagsClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        
        String itemName = ChatUtils.stripColors(clicked.getItemMeta().getDisplayName());
        
        if ("Back to Territory".equals(itemName)) {
            territoryGUI.openTerritoryManagement(player);
            return;
        }
        
        // Find the flag
        for (TerritoryFlag flag : TerritoryFlag.values()) {
            if (flag.getDisplayName().equals(itemName)) {
                Territory territory = plugin.getTerritoryManager().getTerritoryAt(player.getLocation());
                if (territory != null) {
                    boolean newValue = !territory.hasFlag(flag);
                    if (plugin.getTerritoryManager().setTerritoryFlag(player, territory.getName(), flag, newValue)) {
                        ChatUtils.sendSuccess(player, "Flag " + flag.getDisplayName() + " " + (newValue ? "enabled" : "disabled"));
                        territoryGUI.openFlagsGUI(player, territory); // Refresh GUI
                    } else {
                        ChatUtils.sendError(player, "You don't have permission to change this flag!");
                    }
                }
                break;
            }
        }
    }
    
    private void handleRolesClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        
        String itemName = ChatUtils.stripColors(clicked.getItemMeta().getDisplayName());
        
        if ("Back to Territory".equals(itemName)) {
            territoryGUI.openTerritoryManagement(player);
        } else if ("Trust Player".equals(itemName)) {
            player.closeInventory();
            ChatUtils.sendInfo(player, "Use /territory trust <territory> <player> <role> to trust a player!");
        } else if ("Role Templates".equals(itemName)) {
            // Open role templates GUI (future feature)
            ChatUtils.sendInfo(player, "Role templates feature coming soon!");
        }
    }
    
    private void handleSubAreasClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        
        String itemName = ChatUtils.stripColors(clicked.getItemMeta().getDisplayName());
        
        if ("Back to Territory".equals(itemName)) {
            territoryGUI.openTerritoryManagement(player);
        } else if ("Create Sub-Area".equals(itemName)) {
            player.closeInventory();
            ChatUtils.sendInfo(player, "Use /territory subarea create <territory> <name> to create a sub-area!");
        } else if ("Selection Tool".equals(itemName)) {
            player.closeInventory();
            player.getInventory().addItem(createSelectionTool());
            ChatUtils.sendInfo(player, "Selection tool added to your inventory!");
        }
    }
    
    private void handleRentalClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        
        String itemName = ChatUtils.stripColors(clicked.getItemMeta().getDisplayName());
        
        if ("Back to Territory".equals(itemName)) {
            territoryGUI.openTerritoryManagement(player);
        } else if ("Rental Settings".equals(itemName)) {
            ChatUtils.sendInfo(player, "Rental settings feature coming soon!");
        } else if ("Tenant Management".equals(itemName)) {
            ChatUtils.sendInfo(player, "Tenant management feature coming soon!");
        }
    }
    
    private void handleClaimingToolsClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        
        String itemName = ChatUtils.stripColors(clicked.getItemMeta().getDisplayName());
        
        switch (itemName.toLowerCase()) {
            case "selection tool" -> {
                player.closeInventory();
                player.getInventory().addItem(createSelectionTool());
                ChatUtils.sendInfo(player, "Selection tool added to your inventory!");
            }
            case "claim current chunk" -> {
                player.closeInventory();
                ChatUtils.sendInfo(player, "Use /territory claim <name> to claim this chunk!");
            }
            case "radius claim" -> {
                player.closeInventory();
                ChatUtils.sendInfo(player, "Radius claiming feature coming soon!");
            }
            case "auto claim" -> {
                player.closeInventory();
                ChatUtils.sendInfo(player, "Auto claiming feature coming soon!");
            }
            case "close" -> {
                player.closeInventory();
            }
        }
    }
    
    private void openTerritoryDetails(Player player, Territory territory) {
        // This would open a detailed territory management GUI
        ChatUtils.sendInfo(player, "Territory details GUI coming soon!");
        for (String line : territory.getFormattedInfo()) {
            player.sendMessage(line);
        }
    }
    
    private void showClaimingLimits(Player player) {
        ClaimLimits limits = plugin.getTerritoryManager().getClaimLimits();
        
        player.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(ChatUtils.colorize("&6&lYour Claiming Limits"));
        player.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(ChatUtils.colorize("&6Chunks: &e" + limits.getCurrentChunks(player) + "/" + limits.getMaxChunks(player)));
        player.sendMessage(ChatUtils.colorize("&6Territories: &e" + limits.getCurrentTerritories(player) + "/" + limits.getMaxTerritories(player)));
        player.sendMessage(ChatUtils.colorize("&6Remaining Chunks: &e" + limits.getRemainingChunks(player)));
        player.sendMessage(ChatUtils.colorize("&6Remaining Territories: &e" + limits.getRemainingTerritories(player)));
        player.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        player.closeInventory();
    }
    
    private ItemStack createSelectionTool() {
        ItemStack tool = new ItemStack(Material.GOLDEN_AXE);
        tool.getItemMeta().setDisplayName(ChatUtils.colorize("&6Territory Selection Tool"));
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.colorize("&7Left-click to set corner 1"));
        lore.add(ChatUtils.colorize("&7Right-click to set corner 2"));
        tool.getItemMeta().setLore(lore);
        return tool;
    }
    
    public TerritoryGUI getTerritoryGUI() {
        return territoryGUI;
    }
}