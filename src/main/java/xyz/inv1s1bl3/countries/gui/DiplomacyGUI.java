package xyz.inv1s1bl3.countries.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.country.Country;
import xyz.inv1s1bl3.countries.core.diplomacy.DiplomaticRelation;
import xyz.inv1s1bl3.countries.core.diplomacy.RelationType;
import xyz.inv1s1bl3.countries.core.diplomacy.TradeAgreement;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * GUI interface for diplomacy management.
 */
public class DiplomacyGUI {
    
    private final CountriesPlugin plugin;
    
    public DiplomacyGUI(CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Open the main diplomacy management GUI
     */
    public void openDiplomacyManagement(Player player) {
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) {
            ChatUtils.sendPrefixedConfigMessage(player, "country.not-member");
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, 
                ChatUtils.colorize("&6&l🤝 Diplomacy Management"));
        
        // Current Relations Overview
        Set<DiplomaticRelation> relations = plugin.getDiplomacyManager().getCountryRelations(country.getName());
        ItemStack relationsItem = createItem(Material.BOOK, "&6&lDiplomatic Relations", 
                "&7Total Relations: &e" + relations.size(),
                "&7Allies: &a" + plugin.getDiplomacyManager().getAllies(country.getName()).size(),
                "&7Enemies: &c" + plugin.getDiplomacyManager().getEnemies(country.getName()).size(),
                "&7Click to view all relations");
        gui.setItem(10, relationsItem);
        
        // Propose Alliance
        ItemStack allianceItem = createItem(Material.EMERALD, "&6&lPropose Alliance", 
                "&7Form alliances with other countries",
                "&7for mutual benefits and protection",
                "&7Click to propose alliance");
        gui.setItem(12, allianceItem);
        
        // Declare War
        ItemStack warItem = createItem(Material.IRON_SWORD, "&6&lDeclare War", 
                "&7Declare war on enemy countries",
                "&7and engage in conflicts",
                "&7Click to declare war");
        gui.setItem(14, warItem);
        
        // Trade Agreements
        Set<TradeAgreement> agreements = plugin.getDiplomacyManager().getCountryTradeAgreements(country.getName());
        ItemStack tradeItem = createItem(Material.CHEST, "&6&lTrade Agreements", 
                "&7Active Agreements: &e" + agreements.size(),
                "&7Manage trade relationships",
                "&7and economic partnerships",
                "&7Click to manage trade");
        gui.setItem(16, tradeItem);
        
        // Set Neutral
        ItemStack neutralItem = createItem(Material.WHITE_BANNER, "&6&lSet Neutral", 
                "&7Set neutral relations with",
                "&7other countries",
                "&7Click to set neutral");
        gui.setItem(28, neutralItem);
        
        // Diplomacy History
        ItemStack historyItem = createItem(Material.WRITABLE_BOOK, "&6&lDiplomacy History", 
                "&7View diplomatic history",
                "&7and past relations",
                "&7Click to view history");
        gui.setItem(30, historyItem);
        
        // Pending Proposals
        ItemStack proposalsItem = createItem(Material.BELL, "&6&lPending Proposals", 
                "&7View and respond to",
                "&7diplomatic proposals",
                "&7Click to view proposals");
        gui.setItem(32, proposalsItem);
        
        // Diplomacy Settings
        ItemStack settingsItem = createItem(Material.REDSTONE, "&6&lDiplomacy Settings", 
                "&7Configure diplomatic policies",
                "&7and relationship rules",
                "&7Click to configure");
        gui.setItem(34, settingsItem);
        
        // Close button
        ItemStack closeItem = createItem(Material.BARRIER, "&c&lClose", 
                "&7Close this menu");
        gui.setItem(49, closeItem);
        
        player.openInventory(gui);
    }
    
    /**
     * Open relations list GUI
     */
    public void openRelationsList(Player player, int page) {
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) return;
        
        Inventory gui = Bukkit.createInventory(null, 54, 
                ChatUtils.colorize("&6&lDiplomatic Relations"));
        
        List<DiplomaticRelation> relations = new ArrayList<>(plugin.getDiplomacyManager().getCountryRelations(country.getName()));
        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, relations.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            DiplomaticRelation relation = relations.get(i);
            String otherCountry = relation.getOtherCountry(country.getName());
            
            Material material = getRelationMaterial(relation.getRelationType());
            String status = relation.isActive() ? "&aActive" : "&7Inactive";
            
            ItemStack relationItem = createItem(material, "&e" + otherCountry,
                    "&7Relation: &e" + relation.getRelationType().getDisplayName(),
                    "&7Status: " + status,
                    "&7Established: &e" + relation.getDaysSinceEstablishment() + " days ago",
                    "",
                    "&6Left-click to manage",
                    "&6Right-click for options");
            
            gui.setItem(i - startIndex, relationItem);
        }
        
        // Navigation
        if (page > 0) {
            ItemStack prevItem = createItem(Material.ARROW, "&ePrevious Page");
            gui.setItem(45, prevItem);
        }
        
        if (endIndex < relations.size()) {
            ItemStack nextItem = createItem(Material.ARROW, "&eNext Page");
            gui.setItem(53, nextItem);
        }
        
        ItemStack backItem = createItem(Material.BARRIER, "&cBack to Diplomacy");
        gui.setItem(49, backItem);
        
        player.openInventory(gui);
    }
    
    /**
     * Open country selection GUI for diplomatic actions
     */
    public void openCountrySelection(Player player, String action) {
        Country playerCountry = plugin.getCountryManager().getPlayerCountry(player);
        if (playerCountry == null) return;
        
        Inventory gui = Bukkit.createInventory(null, 54, 
                ChatUtils.colorize("&6&lSelect Country - " + ChatUtils.capitalizeWords(action)));
        
        List<Country> countries = new ArrayList<>();
        for (Country country : plugin.getCountryManager().getAllCountries()) {
            if (!country.getName().equals(playerCountry.getName())) {
                countries.add(country);
            }
        }
        
        for (int i = 0; i < Math.min(countries.size(), 45); i++) {
            Country country = countries.get(i);
            
            // Get current relation
            DiplomaticRelation relation = plugin.getDiplomacyManager()
                    .getRelation(playerCountry.getName(), country.getName());
            
            Material material = relation != null ? 
                    getRelationMaterial(relation.getRelationType()) : Material.GRAY_BANNER;
            
            String relationStatus = relation != null ? 
                    relation.getRelationType().getDisplayName() : "No Relation";
            
            ItemStack countryItem = createItem(material, "&e" + country.getName(),
                    "&7Government: &e" + country.getGovernmentType().getDisplayName(),
                    "&7Citizens: &e" + country.getCitizenCount(),
                    "&7Current Relation: &e" + relationStatus,
                    "",
                    "&6Click to " + action);
            
            gui.setItem(i, countryItem);
        }
        
        ItemStack backItem = createItem(Material.ARROW, "&eBack to Diplomacy");
        gui.setItem(49, backItem);
        
        player.openInventory(gui);
    }
    
    /**
     * Get material for relation type
     */
    private Material getMaterialForRelationType(RelationType type) {
        return switch (type) {
            case ALLIED -> Material.EMERALD_BLOCK;
            case FRIENDLY -> Material.LIME_BANNER;
            case NEUTRAL -> Material.WHITE_BANNER;
            case HOSTILE -> Material.ORANGE_BANNER;
            case AT_WAR -> Material.RED_BANNER;
            case TRADE_PARTNER -> Material.CHEST;
            case VASSAL -> Material.IRON_BLOCK;
            case OVERLORD -> Material.GOLD_BLOCK;
        };
    }
    
    /**
     * Get material for relation type
     */
    private Material getRelationMaterial(RelationType type) {
        return getMaterialForRelationType(type);
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