package xyz.inv1s1bl3.countries.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.country.Country;
import xyz.inv1s1bl3.countries.core.territory.*;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GUI interface for territory management.
 */
public class TerritoryGUI {
    
    private final CountriesPlugin plugin;
    
    public TerritoryGUI(CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Open the main territory management GUI
     */
    public void openTerritoryManagement(Player player) {
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) {
            ChatUtils.sendPrefixedConfigMessage(player, "country.not-member");
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, 
                ChatUtils.colorize("&6&lTerritory Management"));
        
        // Territory List
        ItemStack listItem = createItem(Material.MAP, "&6&lYour Territories", 
                "&7View and manage all", "&7your country's territories");
        gui.setItem(10, listItem);
        
        // Claim Tool
        ItemStack claimItem = createItem(Material.GOLDEN_SHOVEL, "&6&lClaiming Tool", 
                "&7Get tools for claiming", "&7and managing territories");
        gui.setItem(12, claimItem);
        
        // Territory Info
        Territory currentTerritory = plugin.getTerritoryManager().getTerritoryAt(player.getLocation());
        if (currentTerritory != null) {
            ItemStack infoItem = createItem(Material.BOOK, "&6&lCurrent Territory", 
                    "&7Territory: &e" + currentTerritory.getName(),
                    "&7Type: &e" + currentTerritory.getType().getDisplayName(),
                    "&7Chunks: &e" + currentTerritory.getChunkCount(),
                    "&7Click to manage");
            gui.setItem(14, infoItem);
        } else {
            ItemStack wildItem = createItem(Material.GRASS_BLOCK, "&6&lWilderness", 
                    "&7You are currently in", "&7unclaimed wilderness",
                    "&7Click to claim this area");
            gui.setItem(14, wildItem);
        }
        
        // Claiming Limits
        ClaimLimits limits = plugin.getTerritoryManager().getClaimLimits();
        ItemStack limitsItem = createItem(Material.BARRIER, "&6&lClaiming Limits", 
                "&7Chunks: &e" + limits.getCurrentChunks(player) + "/" + limits.getMaxChunks(player),
                "&7Territories: &e" + limits.getCurrentTerritories(player) + "/" + limits.getMaxTerritories(player),
                "&7Remaining Chunks: &e" + limits.getRemainingChunks(player),
                "&7Remaining Territories: &e" + limits.getRemainingTerritories(player));
        gui.setItem(16, limitsItem);
        
        // Border Visualization
        ItemStack borderItem = createItem(Material.GLOWSTONE_DUST, "&6&lBorder Visualization", 
                "&7Toggle particle borders", "&7for territory boundaries");
        gui.setItem(28, borderItem);
        
        // Flags Management
        ItemStack flagsItem = createItem(Material.WHITE_BANNER, "&6&lFlags & Permissions", 
                "&7Manage territory flags", "&7and player permissions");
        gui.setItem(30, flagsItem);
        
        // Sub-Areas
        ItemStack subAreaItem = createItem(Material.ITEM_FRAME, "&6&lSub-Areas", 
                "&7Create and manage", "&7sub-areas within territories");
        gui.setItem(32, subAreaItem);
        
        // Rental System
        ItemStack rentalItem = createItem(Material.EMERALD, "&6&lRental System", 
                "&7Set up areas for rent", "&7and manage tenants");
        gui.setItem(34, rentalItem);
        
        // Close button
        ItemStack closeItem = createItem(Material.BARRIER, "&c&lClose", 
                "&7Close this menu");
        gui.setItem(49, closeItem);
        
        player.openInventory(gui);
    }
    
    /**
     * Open territory list GUI
     */
    public void openTerritoryList(Player player, int page) {
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) return;
        
        Inventory gui = Bukkit.createInventory(null, 54, 
                ChatUtils.colorize("&6&lTerritories - " + country.getName()));
        
        List<Territory> territories = new ArrayList<>(plugin.getTerritoryManager().getCountryTerritories(country.getName()));
        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, territories.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            Territory territory = territories.get(i);
            
            Material material = getMaterialForType(territory.getType());
            String status = territory.isActive() ? "&aActive" : "&7Inactive";
            
            ItemStack territoryItem = createItem(material, "&e" + territory.getName(),
                    "&7Type: &e" + territory.getType().getDisplayName(),
                    "&7Chunks: &e" + territory.getChunkCount(),
                    "&7Sub-Areas: &e" + territory.getSubAreas().size(),
                    "&7Status: " + status,
                    "&7Daily Upkeep: &e" + ChatUtils.formatCurrency(territory.getUpkeepCost()),
                    "",
                    "&6Left-click to manage",
                    "&6Right-click to teleport");
            
            gui.setItem(i - startIndex, territoryItem);
        }
        
        // Navigation and controls
        if (page > 0) {
            ItemStack prevItem = createItem(Material.ARROW, "&ePrevious Page");
            gui.setItem(45, prevItem);
        }
        
        if (endIndex < territories.size()) {
            ItemStack nextItem = createItem(Material.ARROW, "&eNext Page");
            gui.setItem(53, nextItem);
        }
        
        // Create new territory
        ItemStack createItem = createItem(Material.NETHER_STAR, "&a&lCreate Territory", 
                "&7Create a new territory", "&7at your current location");
        gui.setItem(49, createItem);
        
        player.openInventory(gui);
    }
    
    /**
     * Open territory flags GUI
     */
    public void openFlagsGUI(Player player, Territory territory) {
        Inventory gui = Bukkit.createInventory(null, 54, 
                ChatUtils.colorize("&6&lFlags - " + territory.getName()));
        
        TerritoryFlag[] flags = TerritoryFlag.values();
        
        for (int i = 0; i < Math.min(flags.length, 45); i++) {
            TerritoryFlag flag = flags[i];
            boolean enabled = territory.hasFlag(flag);
            
            Material material = enabled ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
            String status = enabled ? "&aEnabled" : "&cDisabled";
            
            ItemStack flagItem = createItem(material, "&e" + flag.getDisplayName(),
                    "&7Status: " + status,
                    "&7Description: &f" + flag.getDescription(),
                    "",
                    "&6Click to toggle");
            
            gui.setItem(i, flagItem);
        }
        
        // Back button
        ItemStack backItem = createItem(Material.ARROW, "&eBack to Territory");
        gui.setItem(49, backItem);
        
        player.openInventory(gui);
    }
    
    /**
     * Open roles management GUI
     */
    public void openRolesGUI(Player player, Territory territory) {
        Inventory gui = Bukkit.createInventory(null, 54, 
                ChatUtils.colorize("&6&lRoles - " + territory.getName()));
        
        Map<UUID, TerritoryRole> playerRoles = territory.getAllPlayerRoles();
        int slot = 0;
        
        for (Map.Entry<UUID, TerritoryRole> entry : playerRoles.entrySet()) {
            if (slot >= 45) break;
            
            UUID playerUUID = entry.getKey();
            TerritoryRole role = entry.getValue();
            
            Player rolePlayer = Bukkit.getPlayer(playerUUID);
            String playerName = rolePlayer != null ? rolePlayer.getName() : 
                    Bukkit.getOfflinePlayer(playerUUID).getName();
            
            Material material = rolePlayer != null ? Material.PLAYER_HEAD : Material.SKELETON_SKULL;
            String status = rolePlayer != null ? "&aOnline" : "&7Offline";
            
            ItemStack roleItem = createItem(material, "&e" + playerName,
                    "&7Role: &e" + role.getDisplayName(),
                    "&7Status: " + status,
                    "",
                    "&6Left-click to change role",
                    "&6Right-click to remove");
            
            gui.setItem(slot++, roleItem);
        }
        
        // Add player button
        ItemStack addItem = createItem(Material.NETHER_STAR, "&a&lTrust Player", 
                "&7Trust a new player", "&7in this territory");
        gui.setItem(45, addItem);
        
        // Role templates
        ItemStack templatesItem = createItem(Material.BOOK, "&6&lRole Templates", 
                "&7View and edit role", "&7permission templates");
        gui.setItem(47, templatesItem);
        
        // Back button
        ItemStack backItem = createItem(Material.ARROW, "&eBack to Territory");
        gui.setItem(49, backItem);
        
        player.openInventory(gui);
    }
    
    /**
     * Open sub-areas GUI
     */
    public void openSubAreasGUI(Player player, Territory territory) {
        Inventory gui = Bukkit.createInventory(null, 54, 
                ChatUtils.colorize("&6&lSub-Areas - " + territory.getName()));
        
        Map<String, SubArea> subAreas = territory.getSubAreas();
        int slot = 0;
        
        for (SubArea subArea : subAreas.values()) {
            if (slot >= 45) break;
            
            Material material = subArea.isForRent() ? Material.EMERALD_BLOCK : Material.STONE;
            String rentStatus = subArea.isForRent() ? 
                    (subArea.getCurrentTenant() != null ? "&cRented" : "&aAvailable") : "&7Not for rent";
            
            ItemStack subAreaItem = createItem(material, "&e" + subArea.getName(),
                    "&7Size: &e" + subArea.getAreaSize() + " blocks",
                    "&7Rent Status: " + rentStatus,
                    subArea.isForRent() ? "&7Rent Price: &e" + ChatUtils.formatCurrency(subArea.getRentPrice()) : "",
                    "",
                    "&6Left-click to manage",
                    "&6Right-click to teleport");
            
            gui.setItem(slot++, subAreaItem);
        }
        
        // Create sub-area button
        ItemStack createItem = createItem(Material.NETHER_STAR, "&a&lCreate Sub-Area", 
                "&7Create a new sub-area", "&7within this territory");
        gui.setItem(45, createItem);
        
        // Selection tool
        ItemStack selectionItem = createItem(Material.GOLDEN_AXE, "&6&lSelection Tool", 
                "&7Get selection tool to", "&7define sub-area boundaries");
        gui.setItem(47, selectionItem);
        
        // Back button
        ItemStack backItem = createItem(Material.ARROW, "&eBack to Territory");
        gui.setItem(49, backItem);
        
        player.openInventory(gui);
    }
    
    /**
     * Open rental management GUI
     */
    public void openRentalGUI(Player player, Territory territory) {
        Inventory gui = Bukkit.createInventory(null, 54, 
                ChatUtils.colorize("&6&lRental Management - " + territory.getName()));
        
        Map<String, SubArea> subAreas = territory.getSubAreas();
        int slot = 0;
        
        for (SubArea subArea : subAreas.values()) {
            if (slot >= 45) break;
            
            Material material;
            String status;
            List<String> lore = new ArrayList<>();
            
            if (subArea.isForRent()) {
                if (subArea.getCurrentTenant() != null) {
                    material = Material.RED_CONCRETE;
                    status = "&cRented";
                    lore.add("&7Tenant: &e" + Bukkit.getOfflinePlayer(subArea.getCurrentTenant()).getName());
                    lore.add("&7Expires: &e" + subArea.getDaysUntilRentExpiry() + " days");
                } else {
                    material = Material.GREEN_CONCRETE;
                    status = "&aAvailable";
                }
                lore.add("&7Price: &e" + ChatUtils.formatCurrency(subArea.getRentPrice()));
                lore.add("&7Duration: &e" + (subArea.getRentDuration() / 86400000L) + " days");
            } else {
                material = Material.GRAY_CONCRETE;
                status = "&7Not for rent";
            }
            
            lore.add("");
            lore.add("&6Left-click to configure");
            lore.add("&6Right-click to toggle rental");
            
            List<String> allLore = new ArrayList<>();
            allLore.add("&7Status: " + status);
            allLore.addAll(lore);
            
            ItemStack rentalItem = createItem(material, "&e" + subArea.getName(),
                    allLore.toArray(new String[0]));
            
            gui.setItem(slot++, rentalItem);
        }
        
        // Rental settings
        ItemStack settingsItem = createItem(Material.REDSTONE, "&6&lRental Settings", 
                "&7Configure default rental", "&7prices and durations");
        gui.setItem(45, settingsItem);
        
        // Tenant management
        ItemStack tenantsItem = createItem(Material.PLAYER_HEAD, "&6&lTenant Management", 
                "&7Manage current tenants", "&7and rental agreements");
        gui.setItem(47, tenantsItem);
        
        // Back button
        ItemStack backItem = createItem(Material.ARROW, "&eBack to Territory");
        gui.setItem(49, backItem);
        
        player.openInventory(gui);
    }
    
    /**
     * Open claiming tool GUI
     */
    public void openClaimingGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, 
                ChatUtils.colorize("&6&lClaiming Tools"));
        
        // Selection tool
        ItemStack selectionTool = createItem(Material.GOLDEN_SHOVEL, "&6&lSelection Tool", 
                "&7Left-click to set corner 1", "&7Right-click to set corner 2",
                "&7Use with /territory claim <name>");
        gui.setItem(10, selectionTool);
        
        // Claim current chunk
        ItemStack claimChunk = createItem(Material.GRASS_BLOCK, "&6&lClaim Current Chunk", 
                "&7Claim the chunk you're", "&7currently standing in");
        gui.setItem(12, claimChunk);
        
        // Radius claim
        ItemStack radiusClaim = createItem(Material.BLAZE_POWDER, "&6&lRadius Claim", 
                "&7Claim chunks in a radius", "&7around your location");
        gui.setItem(14, radiusClaim);
        
        // Auto claim
        ItemStack autoClaim = createItem(Material.COMPASS, "&6&lAuto Claim", 
                "&7Automatically claim chunks", "&7as you walk around");
        gui.setItem(16, autoClaim);
        
        // Close button
        ItemStack closeItem = createItem(Material.BARRIER, "&c&lClose");
        gui.setItem(22, closeItem);
        
        player.openInventory(gui);
    }
    
    /**
     * Get material for territory type
     */
    private Material getMaterialForType(TerritoryType type) {
        return switch (type) {
            case RESIDENTIAL -> Material.OAK_PLANKS;
            case COMMERCIAL -> Material.EMERALD_BLOCK;
            case INDUSTRIAL -> Material.IRON_BLOCK;
            case MILITARY -> Material.OBSIDIAN;
            case AGRICULTURAL -> Material.WHEAT;
            case RECREATIONAL -> Material.GRASS_BLOCK;
            case GOVERNMENT -> Material.GOLD_BLOCK;
            case WILDERNESS -> Material.DIRT;
        };
    }
    
    /**
     * Create an item with name and lore
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatUtils.colorize(name));
            
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    if (!line.isEmpty()) {
                        loreList.add(ChatUtils.colorize(line));
                    }
                }
                meta.setLore(loreList);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
}