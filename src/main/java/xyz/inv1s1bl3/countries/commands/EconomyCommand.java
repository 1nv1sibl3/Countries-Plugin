package xyz.inv1s1bl3.countries.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.country.CitizenRole;
import xyz.inv1s1bl3.countries.core.country.Country;
import xyz.inv1s1bl3.countries.core.economy.BankAccount;
import xyz.inv1s1bl3.countries.core.economy.Transaction;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles all economy-related commands.
 * Provides functionality for managing money, taxes, and salaries.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
public final class EconomyCommand implements CommandExecutor, TabCompleter {
    
    private final CountriesPlugin plugin;
    
    public EconomyCommand(@NotNull final CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull final CommandSender sender, 
                           @NotNull final Command command, 
                           @NotNull final String label, 
                           @NotNull final String[] args) {
        
        if (!(sender instanceof Player)) {
            ChatUtils.sendError(sender, this.plugin.getMessage("general.player-only"));
            return true;
        }
        
        final Player player = (Player) sender;
        
        if (args.length == 0) {
            this.sendHelpMessage(player);
            return true;
        }
        
        final String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "balance":
            case "bal":
                this.handleBalanceCommand(player, args);
                break;
            case "pay":
                this.handlePayCommand(player, args);
                break;
            case "tax":
                this.handleTaxCommand(player, args);
                break;
            case "salary":
                this.handleSalaryCommand(player, args);
                break;
            case "history":
                this.handleHistoryCommand(player, args);
                break;
            case "top":
                this.handleTopCommand(player, args);
                break;
            case "stats":
                this.handleStatsCommand(player);
                break;
            case "gui":
                this.handleGuiCommand(player);
                break;
            default:
                ChatUtils.sendError(player, this.plugin.getMessage("general.unknown-command"));
                break;
        }
        
        return true;
    }
    
    /**
     * Handle the balance subcommand.
     */
    private void handleBalanceCommand(@NotNull final Player player, @NotNull final String[] args) {
        if (args.length == 1) {
            // Show own balance
            final double balance = this.plugin.getEconomyManager().getBalance(player.getUniqueId());
            ChatUtils.sendSuccess(player, this.plugin.getMessage("economy.balance-self", 
                "balance", String.format("%.2f", balance)));
        } else {
            // Show other player's balance
            if (!player.hasPermission("countries.economy.balance.others")) {
                ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
                return;
            }
            
            final OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            final double balance = this.plugin.getEconomyManager().getBalance(target.getUniqueId());
            ChatUtils.sendSuccess(player, this.plugin.getMessage("economy.balance-other", 
                "player", target.getName() != null ? target.getName() : "Unknown",
                "balance", String.format("%.2f", balance)));
        }
    }
    
    /**
     * Handle the pay subcommand.
     */
    private void handlePayCommand(@NotNull final Player player, @NotNull final String[] args) {
        if (!player.hasPermission("countries.economy.pay")) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        if (args.length < 3) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.invalid-syntax", 
                "usage", "/economy pay <player> <amount>"));
            return;
        }
        
        final Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.player-not-found", "player", args[1]));
            return;
        }
        
        if (target.equals(player)) {
            ChatUtils.sendError(player, "&cYou cannot pay yourself!");
            return;
        }
        
        final double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (final NumberFormatException exception) {
            ChatUtils.sendError(player, "&cInvalid amount!");
            return;
        }
        
        if (amount <= 0) {
            ChatUtils.sendError(player, "&cAmount must be positive!");
            return;
        }
        
        final double maxPayment = this.plugin.getConfigManager()
            .getConfigValue("economy.max-payment-amount", 10000.0);
        if (amount > maxPayment) {
            ChatUtils.sendError(player, "&cAmount exceeds maximum payment limit of $" + maxPayment);
            return;
        }
        
        if (this.plugin.getEconomyManager().transfer(player.getUniqueId(), target.getUniqueId(), 
                amount, "Payment from " + player.getName())) {
            ChatUtils.sendSuccess(player, this.plugin.getMessage("economy.payment-sent", 
                "amount", String.format("%.2f", amount), "player", target.getName()));
            ChatUtils.sendSuccess(target, this.plugin.getMessage("economy.payment-received", 
                "amount", String.format("%.2f", amount), "player", player.getName()));
        } else {
            ChatUtils.sendError(player, this.plugin.getMessage("general.insufficient-funds", 
                "amount", String.format("%.2f", amount)));
        }
    }
    
    /**
     * Handle the tax subcommand.
     */
    private void handleTaxCommand(@NotNull final Player player, @NotNull final String[] args) {
        final Country country = this.plugin.getCountryManager().getPlayerCountry(player.getUniqueId());
        if (country == null) {
            ChatUtils.sendError(player, this.plugin.getMessage("country.not-member"));
            return;
        }
        
        if (args.length < 2) {
            // Show current tax rate
            ChatUtils.sendInfo(player, "&eCurrent tax rate: &f" + (country.getTaxRate() * 100) + "%");
            return;
        }
        
        final String subCommand = args[1].toLowerCase();
        
        if (subCommand.equals("set")) {
            if (!player.hasPermission("countries.economy.tax.set")) {
                ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
                return;
            }
            
            final CitizenRole playerRole = country.getCitizenRole(player.getUniqueId());
            if (playerRole == null || playerRole != CitizenRole.LEADER) {
                ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
                return;
            }
            
            if (args.length < 3) {
                ChatUtils.sendError(player, this.plugin.getMessage("general.invalid-syntax", 
                    "usage", "/economy tax set <rate>"));
                return;
            }
            
            final double taxRate;
            try {
                taxRate = Double.parseDouble(args[2]) / 100.0; // Convert percentage to decimal
            } catch (final NumberFormatException exception) {
                ChatUtils.sendError(player, "&cInvalid tax rate!");
                return;
            }
            
            final double maxTaxRate = this.plugin.getConfigManager()
                .getConfigValue("economy.max-tax-rate", 0.25);
            
            if (taxRate < 0 || taxRate > maxTaxRate) {
                ChatUtils.sendError(player, "&cTax rate must be between 0% and " + (maxTaxRate * 100) + "%!");
                return;
            }
            
            if (country.setTaxRate(taxRate)) {
                ChatUtils.sendSuccess(player, this.plugin.getMessage("economy.tax-rate-set", 
                    "rate", String.format("%.1f", taxRate * 100), "country", country.getName()));
            }
            
        } else if (subCommand.equals("collect")) {
            if (!player.hasPermission("countries.economy.tax.collect")) {
                ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
                return;
            }
            
            final CitizenRole playerRole = country.getCitizenRole(player.getUniqueId());
            if (playerRole == null || !playerRole.canManage(CitizenRole.CITIZEN)) {
                ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
                return;
            }
            
            final double collected = this.plugin.getEconomyManager().getTaxSystem().collectCountryTaxes(country);
            ChatUtils.sendSuccess(player, this.plugin.getMessage("economy.tax-collected", 
                "amount", String.format("%.2f", collected), "citizens", String.valueOf(country.getCitizenCount())));
        }
    }
    
    /**
     * Handle the salary subcommand.
     */
    private void handleSalaryCommand(@NotNull final Player player, @NotNull final String[] args) {
        if (!player.hasPermission("countries.economy.salary")) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        final Country country = this.plugin.getCountryManager().getPlayerCountry(player.getUniqueId());
        if (country == null) {
            ChatUtils.sendError(player, this.plugin.getMessage("country.not-member"));
            return;
        }
        
        final CitizenRole playerRole = country.getCitizenRole(player.getUniqueId());
        if (playerRole == null || playerRole != CitizenRole.LEADER) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        if (args.length < 2) {
            // Show current salaries
            ChatUtils.sendMessage(player, ChatUtils.createHeader("Salary Information"));
            for (final CitizenRole role : CitizenRole.values()) {
                final double salary = country.getSalary(role);
                ChatUtils.sendMessage(player, ChatUtils.createListItem(role.getDisplayName(), 
                    "$" + String.format("%.2f", salary)));
            }
            return;
        }
        
        if (args.length < 4 || !args[1].equalsIgnoreCase("set")) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.invalid-syntax", 
                "usage", "/economy salary set <role> <amount>"));
            return;
        }
        
        final CitizenRole role = CitizenRole.fromString(args[2]);
        if (role == null) {
            ChatUtils.sendError(player, "&cInvalid role! Valid roles: LEADER, MINISTER, OFFICER, CITIZEN");
            return;
        }
        
        final double salary;
        try {
            salary = Double.parseDouble(args[3]);
        } catch (final NumberFormatException exception) {
            ChatUtils.sendError(player, "&cInvalid salary amount!");
            return;
        }
        
        if (salary < 0) {
            ChatUtils.sendError(player, "&cSalary cannot be negative!");
            return;
        }
        
        final double maxSalary = this.plugin.getConfigManager()
            .getConfigValue("economy.max-salary-amount", 1000.0);
        if (salary > maxSalary) {
            ChatUtils.sendError(player, "&cSalary exceeds maximum limit of $" + maxSalary);
            return;
        }
        
        country.setSalary(role, salary);
        ChatUtils.sendSuccess(player, this.plugin.getMessage("economy.salary-set", 
            "role", role.getDisplayName(), "amount", String.format("%.2f", salary)));
    }
    
    /**
     * Handle the history subcommand.
     */
    private void handleHistoryCommand(@NotNull final Player player, @NotNull final String[] args) {
        if (!player.hasPermission("countries.economy.history")) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        final int limit = args.length > 1 ? Math.min(Integer.parseInt(args[1]), 20) : 10;
        final List<Transaction> transactions = this.plugin.getEconomyManager()
            .getPlayerTransactions(player.getUniqueId(), limit);
        
        if (transactions.isEmpty()) {
            ChatUtils.sendInfo(player, "&7No transaction history found.");
            return;
        }
        
        ChatUtils.sendMessage(player, ChatUtils.createHeader("Transaction History"));
        for (final Transaction transaction : transactions) {
            ChatUtils.sendMessage(player, "&7" + transaction.getFormattedString());
        }
    }
    
    /**
     * Handle the top subcommand.
     */
    private void handleTopCommand(@NotNull final Player player, @NotNull final String[] args) {
        if (!player.hasPermission("countries.economy.top")) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        final int limit = args.length > 1 ? Math.min(Integer.parseInt(args[1]), 20) : 10;
        final List<UUID> richestPlayers = this.plugin.getEconomyManager().getRichestPlayers(limit);
        
        ChatUtils.sendMessage(player, ChatUtils.createHeader("Richest Players"));
        
        int rank = 1;
        for (final UUID playerId : richestPlayers) {
            final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
            final String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
            final double balance = this.plugin.getEconomyManager().getBalance(playerId);
            
            ChatUtils.sendMessage(player, String.format("&e%d. &f%s &7- &a$%.2f", 
                rank++, playerName, balance));
        }
    }
    
    /**
     * Handle the stats subcommand.
     */
    private void handleStatsCommand(@NotNull final Player player) {
        if (!player.hasPermission("countries.economy.stats")) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        final Map<String, Object> stats = this.plugin.getEconomyManager().getTransactionStatistics();
        final Map<String, Object> taxStats = this.plugin.getEconomyManager().getTaxSystem().getTaxStatistics();
        
        ChatUtils.sendMessage(player, ChatUtils.createHeader("Economy Statistics"));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Total Accounts", stats.get("total_accounts").toString()));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Total Transactions", stats.get("total_transactions").toString()));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Money in Circulation", 
            "$" + String.format("%.2f", (Double) stats.get("total_money_in_circulation"))));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Average Tax Rate", 
            String.format("%.1f%%", (Double) taxStats.get("average_tax_rate") * 100)));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Countries with Taxes", 
            taxStats.get("countries_with_taxes").toString()));
    }
    
    /**
     * Handle the gui subcommand.
     */
    private void handleGuiCommand(@NotNull final Player player) {
        // TODO: Implement GUI opening
        ChatUtils.sendInfo(player, "&7Economy GUI system coming soon!");
    }
    
    /**
     * Send help message to a player.
     */
    private void sendHelpMessage(@NotNull final Player player) {
        ChatUtils.sendMessage(player, ChatUtils.createHeader("Economy Commands"));
        ChatUtils.sendMessage(player, "&e/economy balance [player] &7- Check balance");
        ChatUtils.sendMessage(player, "&e/economy pay <player> <amount> &7- Send money");
        ChatUtils.sendMessage(player, "&e/economy tax [set <rate>|collect] &7- Manage taxes");
        ChatUtils.sendMessage(player, "&e/economy salary [set <role> <amount>] &7- Manage salaries");
        ChatUtils.sendMessage(player, "&e/economy history [limit] &7- View transaction history");
        ChatUtils.sendMessage(player, "&e/economy top [limit] &7- View richest players");
        ChatUtils.sendMessage(player, "&e/economy stats &7- View economy statistics");
        ChatUtils.sendMessage(player, "&e/economy gui &7- Open economy GUI");
    }
    
    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull final CommandSender sender, 
                                    @NotNull final Command command, 
                                    @NotNull final String alias, 
                                    @NotNull final String[] args) {
        
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }
        
        final Player player = (Player) sender;
        
        if (args.length == 1) {
            return Arrays.asList("balance", "pay", "tax", "salary", "history", "top", "stats", "gui")
                .stream()
                .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            final String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "balance":
                case "pay":
                    return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                        
                case "tax":
                    return Arrays.asList("set", "collect")
                        .stream()
                        .filter(sub -> sub.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                        
                case "salary":
                    return Arrays.asList("set")
                        .stream()
                        .filter(sub -> sub.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("salary") && args[1].equalsIgnoreCase("set")) {
            return Arrays.stream(CitizenRole.values())
                .map(role -> role.name().toLowerCase())
                .filter(name -> name.startsWith(args[2].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}