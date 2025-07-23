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
import xyz.inv1s1bl3.countries.core.economy.BankAccount;
import xyz.inv1s1bl3.countries.core.economy.Transaction;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI interface for economy management.
 */
public class EconomyGUI {
    
    private final CountriesPlugin plugin;
    
    public EconomyGUI(CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Open the main economy management GUI
     */
    public void openEconomyManagement(Player player) {
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) {
            ChatUtils.sendPrefixedConfigMessage(player, "country.not-member");
            return;
        }
        
        BankAccount playerAccount = plugin.getEconomyManager().getPlayerAccount(player.getUniqueId());
        BankAccount countryAccount = plugin.getEconomyManager().getCountryAccount(country.getName());
        
        Inventory gui = Bukkit.createInventory(null, 54, 
                ChatUtils.colorize("&6&l💰 Economy Management"));
        
        // Player Balance
        ItemStack balanceItem = createItem(Material.GOLD_INGOT, "&6&lYour Balance", 
                "&7Current Balance: &e" + ChatUtils.formatCurrency(playerAccount != null ? playerAccount.getBalance() : 0),
                "&7Click to view transaction history");
        gui.setItem(10, balanceItem);
        
        // Country Treasury
        ItemStack treasuryItem = createItem(Material.EMERALD_BLOCK, "&6&lCountry Treasury", 
                "&7Treasury Balance: &e" + ChatUtils.formatCurrency(countryAccount != null ? countryAccount.getBalance() : 0),
                "&7Tax Rate: &e" + ChatUtils.formatPercentage(country.getTaxRate()),
                "&7Click to manage treasury");
        gui.setItem(12, treasuryItem);
        
        // Send Money
        ItemStack sendItem = createItem(Material.PAPER, "&6&lSend Money", 
                "&7Send money to other players",
                "&7or country treasury",
                "&7Click to open send menu");
        gui.setItem(14, sendItem);
        
        // Tax Management
        Citizen citizen = country.getCitizen(player.getUniqueId());
        if (citizen != null && citizen.getRole().canManageEconomy()) {
            ItemStack taxItem = createItem(Material.BOOK, "&6&lTax Management", 
                    "&7Current Rate: &e" + ChatUtils.formatPercentage(country.getTaxRate()),
                    "&7Daily Income: &e" + ChatUtils.formatCurrency(country.calculateTaxIncome()),
                    "&7Click to manage taxes");
            gui.setItem(16, taxItem);
        }
        
        // Salary Management
        if (citizen != null && citizen.getRole().canManageEconomy()) {
            ItemStack salaryItem = createItem(Material.DIAMOND, "&6&lSalary Management", 
                    "&7Manage citizen salaries",
                    "&7and role payments",
                    "&7Click to manage salaries");
            gui.setItem(28, salaryItem);
        }
        
        // Transaction History
        ItemStack historyItem = createItem(Material.WRITABLE_BOOK, "&6&lTransaction History", 
                "&7View your recent transactions",
                "&7and payment history",
                "&7Click to view history");
        gui.setItem(30, historyItem);
        
        // Statistics
        ItemStack statsItem = createItem(Material.CLOCK, "&6&lEconomy Statistics", 
                "&7View economy statistics",
                "&7and financial reports",
                "&7Click to view stats");
        gui.setItem(32, statsItem);
        
        // Bank Settings
        if (citizen != null && citizen.getRole().canManageEconomy()) {
            ItemStack bankItem = createItem(Material.CHEST, "&6&lBank Settings", 
                    "&7Configure banking options",
                    "&7and financial policies",
                    "&7Click to configure");
            gui.setItem(34, bankItem);
        }
        
        // Close button
        ItemStack closeItem = createItem(Material.BARRIER, "&c&lClose", 
                "&7Close this menu");
        gui.setItem(49, closeItem);
        
        player.openInventory(gui);
    }
    
    /**
     * Open transaction history GUI
     */
    public void openTransactionHistory(Player player, int page) {
        BankAccount account = plugin.getEconomyManager().getPlayerAccount(player.getUniqueId());
        if (account == null) {
            ChatUtils.sendError(player, "Account not found!");
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, 
                ChatUtils.colorize("&6&lTransaction History"));
        
        List<Transaction> transactions = plugin.getEconomyManager()
                .getAccountTransactions(account.getAccountId(), 45);
        
        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, transactions.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            Transaction transaction = transactions.get(i);
            
            Material material = getTransactionMaterial(transaction);
            String status = transaction.isRecent() ? "&aRecent" : "&7Old";
            
            ItemStack transactionItem = createItem(material, "&e" + transaction.getType().getDisplayName(),
                    "&7Amount: &e" + ChatUtils.formatCurrency(transaction.getAmount()),
                    "&7Date: &e" + transaction.getDaysSinceTransaction() + " days ago",
                    "&7Status: " + status,
                    "&7Description: &f" + transaction.getDescription());
            
            gui.setItem(i - startIndex, transactionItem);
        }
        
        // Navigation
        if (page > 0) {
            ItemStack prevItem = createItem(Material.ARROW, "&ePrevious Page");
            gui.setItem(45, prevItem);
        }
        
        if (endIndex < transactions.size()) {
            ItemStack nextItem = createItem(Material.ARROW, "&eNext Page");
            gui.setItem(53, nextItem);
        }
        
        ItemStack backItem = createItem(Material.BARRIER, "&cBack to Economy");
        gui.setItem(49, backItem);
        
        player.openInventory(gui);
    }
    
    /**
     * Open send money GUI
     */
    public void openSendMoneyGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, 
                ChatUtils.colorize("&6&lSend Money"));
        
        // Quick amounts
        ItemStack[] amounts = {
                createItem(Material.COPPER_INGOT, "&610", "&7Send $10"),
                createItem(Material.IRON_INGOT, "&650", "&7Send $50"),
                createItem(Material.GOLD_INGOT, "&6100", "&7Send $100"),
                createItem(Material.DIAMOND, "&6500", "&7Send $500"),
                createItem(Material.EMERALD, "&61000", "&7Send $1000"),
                createItem(Material.NETHERITE_INGOT, "&65000", "&7Send $5000")
        };
        
        for (int i = 0; i < amounts.length; i++) {
            gui.setItem(10 + i, amounts[i]);
        }
        
        // Custom amount
        ItemStack customItem = createItem(Material.ANVIL, "&6&lCustom Amount", 
                "&7Click to enter custom amount",
                "&7in chat");
        gui.setItem(22, customItem);
        
        player.openInventory(gui);
    }
    
    /**
     * Open tax management GUI
     */
    public void openTaxManagement(Player player) {
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) return;
        
        Inventory gui = Bukkit.createInventory(null, 27, 
                ChatUtils.colorize("&6&lTax Management"));
        
        // Current tax rate
        ItemStack currentItem = createItem(Material.BOOK, "&6&lCurrent Tax Rate", 
                "&7Rate: &e" + ChatUtils.formatPercentage(country.getTaxRate()),
                "&7Daily Income: &e" + ChatUtils.formatCurrency(country.calculateTaxIncome()),
                "&7Citizens: &e" + country.getCitizenCount());
        gui.setItem(13, currentItem);
        
        // Tax rate options
        double[] rates = {0.0, 0.05, 0.10, 0.15, 0.20, 0.25};
        String[] rateNames = {"0%", "5%", "10%", "15%", "20%", "25%"};
        Material[] rateMaterials = {
                Material.WHITE_CONCRETE, Material.LIME_CONCRETE, Material.YELLOW_CONCRETE,
                Material.ORANGE_CONCRETE, Material.RED_CONCRETE, Material.PURPLE_CONCRETE
        };
        
        for (int i = 0; i < rates.length; i++) {
            boolean current = Math.abs(country.getTaxRate() - rates[i]) < 0.001;
            ItemStack rateItem = createItem(rateMaterials[i], 
                    "&6" + rateNames[i] + (current ? " &a(Current)" : ""),
                    "&7Set tax rate to " + rateNames[i],
                    current ? "&aCurrently active" : "&7Click to set");
            gui.setItem(i + 9, rateItem);
        }
        
        // Back button
        ItemStack backItem = createItem(Material.ARROW, "&eBack to Economy");
        gui.setItem(18, backItem);
        
        player.openInventory(gui);
    }
    
    /**
     * Get material for transaction type
     */
    private Material getTransactionMaterial(Transaction transaction) {
        return switch (transaction.getType()) {
            case TRANSFER -> Material.PAPER;
            case TAX_COLLECTION -> Material.BOOK;
            case SALARY_PAYMENT -> Material.DIAMOND;
            case TERRITORY_CLAIM -> Material.GRASS_BLOCK;
            case FINE_PAYMENT -> Material.IRON_BARS;
            case DEPOSIT -> Material.EMERALD;
            case WITHDRAWAL -> Material.REDSTONE;
            default -> Material.GOLD_NUGGET;
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
                    loreList.add(ChatUtils.colorize(line));
                }
                meta.setLore(loreList);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
}