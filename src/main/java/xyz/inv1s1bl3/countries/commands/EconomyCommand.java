package xyz.inv1s1bl3.countries.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.country.Country;
import xyz.inv1s1bl3.countries.core.country.Citizen;
import xyz.inv1s1bl3.countries.core.country.CitizenRole;
import xyz.inv1s1bl3.countries.core.economy.BankAccount;
import xyz.inv1s1bl3.countries.core.economy.Transaction;
import xyz.inv1s1bl3.countries.core.economy.TransactionType;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.*;

/**
 * Handles all economy-related commands.
 */
public class EconomyCommand implements CommandExecutor, TabCompleter {
    
    private final CountriesPlugin plugin;
    
    public EconomyCommand(CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "balance", "bal" -> handleBalance(sender, args);
            case "pay" -> handlePay(sender, args);
            case "tax" -> handleTax(sender, args);
            case "salary" -> handleSalary(sender, args);
            case "history" -> handleHistory(sender, args);
            case "country" -> handleCountryBalance(sender, args);
            case "gui" -> handleGUI(sender, args);
            case "help" -> sendHelp(sender);
            default -> {
                ChatUtils.sendError(sender, "Unknown subcommand. Use /ceconomy help for available commands.");
                sendHelp(sender);
            }
        }
        
        return true;
    }
    
    private void handleBalance(CommandSender sender, String[] args) {
        if (!sender.hasPermission("countries.economy.balance")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        Player target;
        
        if (args.length >= 2) {
            // Check other player's balance
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                ChatUtils.sendPrefixedConfigMessage(sender, "general.player-not-found", args[1]);
                return;
            }
            
            // Check permission to view other balances
            if (!sender.hasPermission("countries.economy.balance.others")) {
                ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
                return;
            }
        } else {
            // Check own balance
            if (!(sender instanceof Player)) {
                ChatUtils.sendError(sender, "Console must specify a player name!");
                return;
            }
            target = (Player) sender;
        }
        
        BankAccount account = plugin.getEconomyManager().getPlayerAccount(target.getUniqueId());
        if (account == null) {
            ChatUtils.sendError(sender, "Account not found!");
            return;
        }
        
        if (target.equals(sender)) {
            ChatUtils.sendPrefixedConfigMessage(sender, "economy.balance", 
                    ChatUtils.formatCurrency(account.getBalance()));
        } else {
            ChatUtils.sendPrefixedConfigMessage(sender, "economy.balance-other", 
                    target.getName(), ChatUtils.formatCurrency(account.getBalance()));
        }
    }
    
    private void handlePay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can send money!");
            return;
        }
        
        if (!sender.hasPermission("countries.economy.pay")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 3) {
            ChatUtils.sendError(sender, "Usage: /ceconomy pay <player> <amount>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.player-not-found", args[1]);
            return;
        }
        
        if (target.equals(player)) {
            ChatUtils.sendPrefixedConfigMessage(sender, "economy.cannot-pay-self");
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.invalid-number", args[2]);
            return;
        }
        
        if (amount <= 0) {
            ChatUtils.sendPrefixedConfigMessage(sender, "economy.invalid-amount");
            return;
        }
        
        BankAccount senderAccount = plugin.getEconomyManager().getPlayerAccount(player.getUniqueId());
        BankAccount targetAccount = plugin.getEconomyManager().getPlayerAccount(target.getUniqueId());
        
        if (senderAccount == null || targetAccount == null) {
            ChatUtils.sendError(sender, "Account error occurred!");
            return;
        }
        
        if (!senderAccount.hasBalance(amount)) {
            ChatUtils.sendPrefixedConfigMessage(sender, "economy.insufficient-funds");
            return;
        }
        
        if (plugin.getEconomyManager().transferMoney(senderAccount, targetAccount, amount, 
                TransactionType.TRANSFER, "Player payment")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "economy.transaction-complete", 
                    ChatUtils.formatCurrency(amount), target.getName());
            ChatUtils.sendPrefixedConfigMessage(target, "economy.transaction-received", 
                    ChatUtils.formatCurrency(amount), player.getName());
        } else {
            ChatUtils.sendError(sender, "Transaction failed. Please try again.");
        }
    }
    
    private void handleTax(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can manage taxes!");
            return;
        }
        
        if (!sender.hasPermission("countries.economy.tax")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-member");
            return;
        }
        
        Citizen citizen = country.getCitizen(player.getUniqueId());
        if (citizen == null || !citizen.getRole().canManageEconomy()) {
            ChatUtils.sendError(sender, "You don't have permission to manage country taxes!");
            return;
        }
        
        if (args.length < 3 || !args[1].equalsIgnoreCase("set")) {
            ChatUtils.sendError(sender, "Usage: /ceconomy tax set <rate>");
            ChatUtils.sendInfo(sender, "Rate should be between 0.0 and " + 
                    plugin.getConfigManager().getConfig().getDouble("economy.max-tax-rate", 0.25));
            return;
        }
        
        double taxRate;
        try {
            taxRate = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.invalid-number", args[2]);
            return;
        }
        
        double maxTaxRate = plugin.getConfigManager().getConfig().getDouble("economy.max-tax-rate", 0.25);
        if (taxRate < 0 || taxRate > maxTaxRate) {
            ChatUtils.sendPrefixedConfigMessage(sender, "economy.tax-rate-invalid", 
                    ChatUtils.formatPercentage(maxTaxRate));
            return;
        }
        
        country.setTaxRate(taxRate);
        ChatUtils.sendPrefixedConfigMessage(sender, "economy.tax-rate-set", 
                ChatUtils.formatPercentage(taxRate));
        
        // Notify all citizens
        for (Citizen c : country.getCitizens()) {
            Player citizenPlayer = Bukkit.getPlayer(c.getPlayerUUID());
            if (citizenPlayer != null && !citizenPlayer.equals(player)) {
                ChatUtils.sendPrefixedMessage(citizenPlayer, 
                        "&6Tax rate changed to " + ChatUtils.formatPercentage(taxRate) + " by " + player.getName());
            }
        }
    }
    
    private void handleSalary(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can manage salaries!");
            return;
        }
        
        if (!sender.hasPermission("countries.economy.salary")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 4) {
            ChatUtils.sendError(sender, "Usage: /ceconomy salary <player> <amount>");
            return;
        }
        
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-member");
            return;
        }
        
        Citizen citizen = country.getCitizen(player.getUniqueId());
        if (citizen == null || !citizen.getRole().canManageEconomy()) {
            ChatUtils.sendError(sender, "You don't have permission to manage salaries!");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUUID = null;
        String targetName = args[1];
        
        if (target != null) {
            targetUUID = target.getUniqueId();
            targetName = target.getName();
        } else {
            // Find offline player in country
            for (Citizen c : country.getCitizens()) {
                if (c.getPlayerName().equalsIgnoreCase(targetName)) {
                    targetUUID = c.getPlayerUUID();
                    break;
                }
            }
        }
        
        if (targetUUID == null) {
            ChatUtils.sendError(sender, "Player not found in your country!");
            return;
        }
        
        Citizen targetCitizen = country.getCitizen(targetUUID);
        if (targetCitizen == null) {
            ChatUtils.sendError(sender, "Player is not a citizen of your country!");
            return;
        }
        
        double salary;
        try {
            salary = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.invalid-number", args[2]);
            return;
        }
        
        if (salary < 0) {
            ChatUtils.sendError(sender, "Salary cannot be negative!");
            return;
        }
        
        targetCitizen.setSalary(salary);
        ChatUtils.sendPrefixedConfigMessage(sender, "economy.salary-set", 
                targetCitizen.getRole().getDisplayName(), ChatUtils.formatCurrency(salary));
        
        if (target != null) {
            ChatUtils.sendPrefixedMessage(target, 
                    "&aYour salary has been set to " + ChatUtils.formatCurrency(salary) + " per day!");
        }
    }
    
    private void handleHistory(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can view transaction history!");
            return;
        }
        
        if (!sender.hasPermission("countries.economy.balance")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        BankAccount account = plugin.getEconomyManager().getPlayerAccount(player.getUniqueId());
        if (account == null) {
            ChatUtils.sendError(sender, "Account not found!");
            return;
        }
        
        List<Transaction> transactions = plugin.getEconomyManager()
                .getAccountTransactions(account.getAccountId(), 10);
        
        if (transactions.isEmpty()) {
            ChatUtils.sendInfo(sender, "No recent transactions found.");
            return;
        }
        
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ChatUtils.colorize("&6&lTransaction History"));
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        for (Transaction transaction : transactions) {
            sender.sendMessage(ChatUtils.colorize("&7" + transaction.getFormattedDescription()));
        }
        
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    private void handleCountryBalance(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can check country balance!");
            return;
        }
        
        if (!sender.hasPermission("countries.economy.balance")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-member");
            return;
        }
        
        BankAccount account = plugin.getEconomyManager().getCountryAccount(country.getName());
        if (account == null) {
            ChatUtils.sendError(sender, "Country account not found!");
            return;
        }
        
        ChatUtils.sendPrefixedConfigMessage(sender, "economy.country-balance", 
                ChatUtils.formatCurrency(account.getBalance()));
    }
    
    private void handleGUI(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can use GUIs!");
            return;
        }
        
        if (!sender.hasPermission("countries.economy.gui")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        plugin.getGUIListener().getEconomyGUI().openEconomyManagement(player);
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ChatUtils.colorize("&6&lEconomy Commands"));
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ChatUtils.colorize("&e/ceconomy balance [player] &7- Check balance"));
        sender.sendMessage(ChatUtils.colorize("&e/ceconomy pay <player> <amount> &7- Send money"));
        sender.sendMessage(ChatUtils.colorize("&e/ceconomy tax set <rate> &7- Set country tax rate"));
        sender.sendMessage(ChatUtils.colorize("&e/ceconomy salary <player> <amount> &7- Set player salary"));
        sender.sendMessage(ChatUtils.colorize("&e/ceconomy history &7- View transaction history"));
        sender.sendMessage(ChatUtils.colorize("&e/ceconomy country &7- Check country balance"));
        sender.sendMessage(ChatUtils.colorize("&e/ceconomy gui &7- Open economy GUI"));
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommands
            String[] subCommands = {"balance", "pay", "tax", "salary", "history", "country", "help"};
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "balance", "pay" -> {
                    // Online players
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(player.getName());
                        }
                    }
                }
                case "tax" -> {
                    if ("set".startsWith(args[1].toLowerCase())) {
                        completions.add("set");
                    }
                }
                case "salary" -> {
                    // Citizens of sender's country
                    if (sender instanceof Player player) {
                        Country country = plugin.getCountryManager().getPlayerCountry(player);
                        if (country != null) {
                            for (Citizen citizen : country.getCitizens()) {
                                if (citizen.getPlayerName().toLowerCase().startsWith(args[1].toLowerCase())) {
                                    completions.add(citizen.getPlayerName());
                                }
                            }
                        }
                    }
                }
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            if ("tax".equals(subCommand) && "set".equals(args[1].toLowerCase())) {
                // Tax rate suggestions
                completions.add("0.05");
                completions.add("0.10");
                completions.add("0.15");
            }
        }
        
        return completions;
    }
}