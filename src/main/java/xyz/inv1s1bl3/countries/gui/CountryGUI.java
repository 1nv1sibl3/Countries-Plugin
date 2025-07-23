package xyz.inv1s1bl3.countries.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.country.Country;
import xyz.inv1s1bl3.countries.core.country.Citizen;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI interface for country management.
 */
public class CountryGUI {
    
    private final CountriesPlugin plugin;
    
    public CountryGUI(CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Open the main country management GUI
     */
    public void openCountryManagement(Player player) {
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) {
            ChatUtils.sendPrefixedConfigMessage(player, "country.not-member");
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, 
                ChatUtils.colorize("&6&l" + country.getFlag() + " " + country.getName() + " Management"));
        
        // Country Info
        ItemStack infoItem = createItem(Material.BOOK, "&6&lCountry Information", 
                "&7Click to view detailed", "&7information about your country");
        gui.setItem(10, infoItem);
        
        // Citizens Management
        ItemStack citizensItem = createItem(Material.PLAYER_HEAD, "&6&lCitizens (&e" + country.getCitizenCount() + "&6)", 
                "&7Manage country citizens,", "&7roles, and permissions");
        gui.setItem(12, citizensItem);
        
        // Territory Management
        ItemStack territoryItem = createItem(Material.MAP, "&6&lTerritories (&e" + country.getTotalTerritories() + "&6)", 
                "&7Manage claimed territories", "&7and land permissions");
        gui.setItem(14, territoryItem);
        
        // Economy Management
        ItemStack economyItem = createItem(Material.GOLD_INGOT, "&6&lEconomy", 
                "&7Treasury: &e" + ChatUtils.formatCurrency(country.getBalance()),
                "&7Tax Rate: &e" + ChatUtils.formatPercentage(country.getTaxRate()));
        gui.setItem(16, economyItem);
        
        // Diplomacy
        ItemStack diplomacyItem = createItem(Material.PAPER, "&6&lDiplomacy", 
                "&7Manage relations with", "&7other countries");
        gui.setItem(28, diplomacyItem);
        
        // Laws & Order
        ItemStack lawItem = createItem(Material.IRON_SWORD, "&6&lLaws & Order", 
                "&7Manage country laws", "&7and law enforcement");
        gui.setItem(30, lawItem);
        
        // Settings
        ItemStack settingsItem = createItem(Material.REDSTONE, "&6&lSettings", 
                "&7Configure country", "&7settings and preferences");
        gui.setItem(32, settingsItem);
        
        // Statistics
        ItemStack statsItem = createItem(Material.CLOCK, "&6&lStatistics", 
                "&7View country statistics", "&7and activity reports");
        gui.setItem(34, statsItem);
        
        // Close button
        ItemStack closeItem = createItem(Material.BARRIER, "&c&lClose", 
                "&7Close this menu");
        gui.setItem(49, closeItem);
        
        player.openInventory(gui);
    }
    
    /**
     * Open citizens management GUI
     */
    public void openCitizensGUI(Player player, int page) {
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) return;
        
        Inventory gui = Bukkit.createInventory(null, 54, 
                ChatUtils.colorize("&6&lCitizens - " + country.getName()));
        
        List<Citizen> citizens = new ArrayList<>(country.getCitizens());
        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, citizens.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            Citizen citizen = citizens.get(i);
            Player citizenPlayer = Bukkit.getPlayer(citizen.getPlayerUUID());
            
            Material material = citizenPlayer != null ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE;
            String status = citizenPlayer != null ? "&aOnline" : "&7Offline";
            
            ItemStack citizenItem = createItem(material, "&e" + citizen.getPlayerName(),
                    "&7Role: &e" + citizen.getRole().getDisplayName(),
                    "&7Status: " + status,
                    "&7Joined: &e" + citizen.getDaysSinceJoining() + " days ago",
                    "&7Salary: &e" + ChatUtils.formatCurrency(citizen.getSalary()) + "/day",
                    "",
                    "&6Left-click to manage",
                    "&6Right-click for options");
            
            gui.setItem(i - startIndex, citizenItem);
        }
        
        // Navigation and controls
        if (page > 0) {
            ItemStack prevItem = createItem(Material.ARROW, "&ePrevious Page");
            gui.setItem(45, prevItem);
        }
        
        if (endIndex < citizens.size()) {
            ItemStack nextItem = createItem(Material.ARROW, "&eNext Page");
            gui.setItem(53, nextItem);
        }
        
        ItemStack backItem = createItem(Material.BARRIER, "&cBack to Main Menu");
        gui.setItem(49, backItem);
        
        player.openInventory(gui);
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
                    loreList.add(ChatUtils.colorize(line));
                }
                meta.setLore(loreList);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
}