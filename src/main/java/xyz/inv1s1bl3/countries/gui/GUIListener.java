package xyz.inv1s1bl3.countries.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.country.Country;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

/**
 * Handles GUI click events for the Countries plugin.
 */
public class GUIListener implements Listener {
    
    private final CountriesPlugin plugin;
    private final CountryGUI countryGUI;
    private final EconomyGUI economyGUI;
    private final DiplomacyGUI diplomacyGUI;
    
    public GUIListener(CountriesPlugin plugin) {
        this.plugin = plugin;
        this.countryGUI = new CountryGUI(plugin);
        this.economyGUI = new EconomyGUI(plugin);
        this.diplomacyGUI = new DiplomacyGUI(plugin);
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
        if (title.contains("Management")) {
            handleCountryManagementClick(event, player);
        } else if (title.contains("Economy Management")) {
            handleEconomyManagementClick(event, player);
        } else if (title.contains("Diplomacy Management")) {
            handleDiplomacyManagementClick(event, player);
        } else if (title.contains("Citizens")) {
            handleCitizensClick(event, player);
        } else if (title.contains("Transaction History")) {
            handleTransactionHistoryClick(event, player);
        } else if (title.contains("Tax Management")) {
            handleTaxManagementClick(event, player);
        }
    }
    
    private void handleCountryManagementClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        
        String itemName = ChatUtils.stripColors(clicked.getItemMeta().getDisplayName());
        
        switch (itemName.toLowerCase()) {
            case "country information" -> {
                player.closeInventory();
                player.performCommand("country info");
            }
            case "citizens" -> {
                countryGUI.openCitizensGUI(player, 0);
            }
            case "territories" -> {
                player.closeInventory();
                player.performCommand("territory list");
            }
            case "economy" -> {
                player.closeInventory();
                economyGUI.openEconomyManagement(player);
            }
            case "diplomacy" -> {
                player.closeInventory();
                diplomacyGUI.openDiplomacyManagement(player);
            }
            case "laws & order" -> {
                player.closeInventory();
                player.performCommand("law list");
            }
            case "statistics" -> {
                player.closeInventory();
                showStatistics(player);
            }
            case "close" -> {
                player.closeInventory();
            }
        }
    }
    
    private void handleCitizensClick(InventoryClickEvent event, Player player) {
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
        } else if ("Back to Main Menu".equals(itemName)) {
            countryGUI.openCountryManagement(player);
        }
    }
    
    private void handleEconomyManagementClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        
        String itemName = ChatUtils.stripColors(clicked.getItemMeta().getDisplayName());
        
        switch (itemName.toLowerCase()) {
            case "your balance" -> {
                economyGUI.openTransactionHistory(player, 0);
            }
            case "country treasury" -> {
                player.closeInventory();
                player.performCommand("ceconomy country");
            }
            case "send money" -> {
                economyGUI.openSendMoneyGUI(player);
            }
            case "tax management" -> {
                economyGUI.openTaxManagement(player);
            }
            case "transaction history" -> {
                economyGUI.openTransactionHistory(player, 0);
            }
            case "close" -> {
                player.closeInventory();
            }
        }
    }
    
    private void handleDiplomacyManagementClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        
        String itemName = ChatUtils.stripColors(clicked.getItemMeta().getDisplayName());
        
        switch (itemName.toLowerCase()) {
            case "diplomatic relations" -> {
                diplomacyGUI.openRelationsList(player, 0);
            }
            case "propose alliance" -> {
                diplomacyGUI.openCountrySelection(player, "alliance");
            }
            case "declare war" -> {
                diplomacyGUI.openCountrySelection(player, "war");
            }
            case "set neutral" -> {
                diplomacyGUI.openCountrySelection(player, "neutral");
            }
            case "close" -> {
                player.closeInventory();
            }
        }
    }
    
    private void handleTransactionHistoryClick(InventoryClickEvent event, Player player) {
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
        } else if ("Back to Economy".equals(itemName)) {
            economyGUI.openEconomyManagement(player);
        }
    }
    
    private void handleTaxManagementClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        
        String itemName = ChatUtils.stripColors(clicked.getItemMeta().getDisplayName());
        
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) return;
        
        // Handle tax rate changes
        if (itemName.contains("%")) {
            String rateStr = itemName.replaceAll("[^0-9]", "");
            try {
                double rate = Double.parseDouble(rateStr) / 100.0;
                country.setTaxRate(rate);
                ChatUtils.sendSuccess(player, "Tax rate set to " + ChatUtils.formatPercentage(rate) + "!");
                economyGUI.openTaxManagement(player); // Refresh GUI
            } catch (NumberFormatException e) {
                ChatUtils.sendError(player, "Invalid tax rate!");
            }
        } else if ("Back to Economy".equals(itemName)) {
            economyGUI.openEconomyManagement(player);
        }
    }
    
    private void showStatistics(Player player) {
        var country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) return;
        
        player.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(ChatUtils.colorize("&6&l📊 " + country.getName() + " Statistics"));
        player.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(ChatUtils.colorize("&6Founded: &e" + country.getDaysSinceFoundation() + " days ago"));
        player.sendMessage(ChatUtils.colorize("&6Citizens: &e" + country.getCitizenCount() + "/" + country.getMaxCitizens()));
        player.sendMessage(ChatUtils.colorize("&6Territories: &e" + country.getTotalTerritories()));
        player.sendMessage(ChatUtils.colorize("&6Treasury: &e" + ChatUtils.formatCurrency(country.getBalance())));
        player.sendMessage(ChatUtils.colorize("&6Tax Income: &e" + ChatUtils.formatCurrency(country.calculateTaxIncome()) + "/day"));
        player.sendMessage(ChatUtils.colorize("&6Activity: " + (country.isActive() ? "&aActive" : "&7Inactive")));
        player.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    public CountryGUI getCountryGUI() {
        return countryGUI;
    }
    
    public EconomyGUI getEconomyGUI() {
        return economyGUI;
    }
    
    public DiplomacyGUI getDiplomacyGUI() {
        return diplomacyGUI;
    }
}