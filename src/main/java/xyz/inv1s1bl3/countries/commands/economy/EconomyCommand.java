package xyz.inv1s1bl3.countries.commands.economy;

import lombok.RequiredArgsConstructor;
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
import xyz.inv1s1bl3.countries.database.entities.Country;
import xyz.inv1s1bl3.countries.database.entities.Transaction;
import xyz.inv1s1bl3.countries.utils.MessageUtil;
import xyz.inv1s1bl3.countries.utils.PermissionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Economy command handler
 */
@RequiredArgsConstructor
public final class EconomyCommand implements CommandExecutor, TabCompleter {
    
    private final CountriesPlugin plugin;
    
    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, 
                           @NotNull final String label, @NotNull final String[] args) {
        
        if (args.length == 0) {
            this.sendHelpMessage(sender);
            return true;
        }
        
        final String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "balance", "bal" -> this.handleBalance(sender, args);
            case "transfer", "pay" -> this.handleTransfer(sender, args);
            case "history" -> this.handleHistory(sender, args);
            case "tax" -> this.handleTax(sender, args);
            case "salary" -> this.handleSalary(sender, args);
            case "treasury" -> this.handleTreasury(sender, args);
            case "stats" -> this.handleStats(sender, args);
            case "report" -> this.handleReport(sender, args);
            case "bankruptcy" -> this.handleBankruptcy(sender, args);
            case "maintenance" -> this.handleMaintenance(sender, args);
            case "help" -> this.sendHelpMessage(sender);
            default -> MessageUtil.sendMessage(sender, "general.invalid-command", 
                    Map.of("usage", "/economy help"));
        }
        
        return true;
    }
    
    private void handleBalance(final CommandSender sender, final String[] args) {
        if (args.length == 1) {
            // Show own balance
            if (!(sender instanceof Player player)) {
                MessageUtil.sendError(sender, "This command can only be used by players!");
                return;
            }
            
            final double balance = this.plugin.getEconomyManager().getPlayerBalance(player.getUniqueId());
            MessageUtil.sendMessage(sender, "economy.balance-self", 
                Map.of("balance", this.plugin.getEconomyManager().formatMoney(balance)));
            
        } else if (args.length == 2) {
            // Show other player's balance
            if (!sender.hasPermission("countries.economy.balance.others")) {
                MessageUtil.sendMessage(sender, "general.no-permission");
                return;
            }
            
            final String targetName = args[1];
            final OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            
            if (!target.hasPlayedBefore()) {
                MessageUtil.sendMessage(sender, "general.player-not-found", Map.of("player", targetName));
                return;
            }
            
            final double balance = this.plugin.getEconomyManager().getPlayerBalance(target.getUniqueId());
            MessageUtil.sendMessage(sender, "economy.balance-other", 
                Map.of(
                    "player", target.getName(),
                    "balance", this.plugin.getEconomyManager().formatMoney(balance)
                ));
        }
    }
    
    private void handleTransfer(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "This command can only be used by players!");
            return;
        }
        
        if (!PermissionUtil.canTransferMoney(player)) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 3) {
            MessageUtil.sendMessage(sender, "economy.transfer-usage");
            return;
        }
        
        final String targetName = args[1];
        final String amountStr = args[2];
        final String description = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "Money transfer";
        
        // Parse amount
        final double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                MessageUtil.sendMessage(sender, "economy.transfer-invalid-amount", Map.of("amount", amountStr));
                return;
            }
        } catch (final NumberFormatException exception) {
            MessageUtil.sendMessage(sender, "economy.transfer-invalid-amount", Map.of("amount", amountStr));
            return;
        }
        
        // Check if transferring to country
        if (targetName.startsWith("country:")) {
            final String countryName = targetName.substring(8);
            final Optional<Country> countryOpt = this.plugin.getCountryManager().getCountryByName(countryName);
            
            if (countryOpt.isEmpty()) {
                MessageUtil.sendError(sender, "Country '" + countryName + "' not found!");
                return;
            }
            
            final Country country = countryOpt.get();
            
            if (this.plugin.getEconomyManager().transferPlayerToCountry(
                player.getUniqueId(), country.getId(), amount, description)) {
                
                MessageUtil.sendMessage(sender, "economy.transfer-success", 
                    Map.of(
                        "amount", this.plugin.getEconomyManager().formatMoney(amount),
                        "target", country.getName() + " Treasury"
                    ));
            } else {
                MessageUtil.sendMessage(sender, "economy.transfer-failed", 
                    Map.of("reason", "Insufficient funds or transfer error"));
            }
            
        } else {
            // Transfer to player
            final OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            
            if (!target.hasPlayedBefore()) {
                MessageUtil.sendMessage(sender, "general.player-not-found", Map.of("player", targetName));
                return;
            }
            
            if (target.getUniqueId().equals(player.getUniqueId())) {
                MessageUtil.sendMessage(sender, "economy.transfer-self");
                return;
            }
            
            if (this.plugin.getEconomyManager().transferPlayerToPlayer(
                player.getUniqueId(), target.getUniqueId(), amount, description)) {
                
                MessageUtil.sendMessage(sender, "economy.transfer-success", 
                    Map.of(
                        "amount", this.plugin.getEconomyManager().formatMoney(amount),
                        "target", target.getName()
                    ));
                
                // Notify target if online
                final Player onlineTarget = target.getPlayer();
                if (onlineTarget != null) {
                    MessageUtil.sendMessage(onlineTarget, "economy.transfer-received", 
                        Map.of(
                            "amount", this.plugin.getEconomyManager().formatMoney(amount),
                            "sender", player.getName()
                        ));
                }
            } else {
                MessageUtil.sendMessage(sender, "economy.transfer-failed", 
                    Map.of("reason", "Insufficient funds or transfer error"));
            }
        }
    }
    
    private void handleHistory(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "This command can only be used by players!");
            return;
        }
        
        final int limit = args.length > 1 ? Math.min(Integer.parseInt(args[1]), 20) : 10;
        final List<Transaction> transactions = this.plugin.getEconomyManager()
            .getTransactionManager().getPlayerTransactionHistory(player.getUniqueId(), limit);
        
        if (transactions.isEmpty()) {
            MessageUtil.sendInfo(sender, "No transaction history found.");
            return;
        }
        
        MessageUtil.sendInfo(sender, "Recent Transactions (" + transactions.size() + "):");
        for (final Transaction transaction : transactions) {
            final String type = transaction.isPlayerToPlayer() ? "P2P" : 
                              transaction.isPlayerToCountry() ? "P2C" : 
                              transaction.isCountryToPlayer() ? "C2P" : "C2C";
            
            final String direction = transaction.involvesPlayer(player.getUniqueId()) ? 
                (transaction.getFromPlayerUuid() != null && transaction.getFromPlayerUuid().equals(player.getUniqueId()) ? "OUT" : "IN") : "---";
            
            MessageUtil.sendInfo(sender, String.format("&7[%s] &e%s &7%s - &f%s", 
                type, direction, 
                this.plugin.getEconomyManager().formatMoney(transaction.getAmount()),
                transaction.getDescription()));
        }
    }
    
    private void handleTax(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "This command can only be used by players!");
            return;
        }
        
        final Optional<xyz.inv1s1bl3.countries.database.entities.Player> playerDataOpt = 
            this.plugin.getCountryManager().getPlayer(player.getUniqueId());
        
        if (playerDataOpt.isEmpty() || !playerDataOpt.get().hasCountry()) {
            MessageUtil.sendMessage(sender, "country.not-in-country");
            return;
        }
        
        final xyz.inv1s1bl3.countries.database.entities.Player playerData = playerDataOpt.get();
        final Optional<Country> countryOpt = this.plugin.getCountryManager().getCountry(playerData.getCountryId());
        
        if (countryOpt.isEmpty()) {
            MessageUtil.sendMessage(sender, "general.database-error");
            return;
        }
        
        final Country country = countryOpt.get();
        
        if (args.length == 1) {
            // Show current tax rate
            MessageUtil.sendInfo(sender, "Current tax rate: " + country.getTaxRate() + "%");
            return;
        }
        
        if (args.length == 2 && args[1].equalsIgnoreCase("collect")) {
            // Manual tax collection (admin only)
            if (!PermissionUtil.hasCountryAdminPermissions(player)) {
                MessageUtil.sendMessage(sender, "general.no-permission");
                return;
            }
            
            final int collected = this.plugin.getEconomyManager().getTaxManager().collectCountryTaxes(country);
            MessageUtil.sendSuccess(sender, "Collected taxes from " + collected + " citizens!");
            return;
        }
        
        if (args.length == 3 && args[1].equalsIgnoreCase("set")) {
            // Set tax rate
            if (!PermissionUtil.hasCountryAdminPermissions(player)) {
                MessageUtil.sendMessage(sender, "general.no-permission");
                return;
            }
            
            try {
                final double newRate = Double.parseDouble(args[2]);
                
                if (this.plugin.getEconomyManager().getTaxManager().setCountryTaxRate(country, newRate)) {
                    MessageUtil.sendMessage(sender, "economy.tax-rate-set", Map.of("rate", String.valueOf(newRate)));
                } else {
                    final double maxRate = this.plugin.getConfigManager().getMainConfig().getDouble("economy.max-tax-rate", 20.0);
                    MessageUtil.sendMessage(sender, "economy.tax-rate-invalid", Map.of("max", String.valueOf(maxRate)));
                }
            } catch (final NumberFormatException exception) {
                MessageUtil.sendError(sender, "Invalid tax rate: " + args[2]);
            }
        }
    }
    
    private void handleSalary(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "This command can only be used by players!");
            return;
        }
        
        final Optional<xyz.inv1s1bl3.countries.database.entities.Player> playerDataOpt = 
            this.plugin.getCountryManager().getPlayer(player.getUniqueId());
        
        if (playerDataOpt.isEmpty() || !playerDataOpt.get().hasCountry()) {
            MessageUtil.sendMessage(sender, "country.not-in-country");
            return;
        }
        
        final xyz.inv1s1bl3.countries.database.entities.Player playerData = playerDataOpt.get();
        final Optional<Country> countryOpt = this.plugin.getCountryManager().getCountry(playerData.getCountryId());
        
        if (countryOpt.isEmpty()) {
            MessageUtil.sendMessage(sender, "general.database-error");
            return;
        }
        
        final Country country = countryOpt.get();
        
        if (args.length == 1) {
            // Show current salary
            final double salary = this.plugin.getEconomyManager().getTaxManager()
                .calculatePlayerSalary(playerData, country);
            MessageUtil.sendInfo(sender, "Your salary: " + 
                this.plugin.getEconomyManager().formatMoney(salary) + " per day");
            return;
        }
        
        if (args.length == 2 && args[1].equalsIgnoreCase("pay")) {
            // Manual salary payment (admin only)
            if (!PermissionUtil.hasCountryAdminPermissions(player)) {
                MessageUtil.sendMessage(sender, "general.no-permission");
                return;
            }
            
            final int paid = this.plugin.getEconomyManager().getTaxManager().payCountrySalaries(country);
            MessageUtil.sendSuccess(sender, "Paid salaries to " + paid + " members!");
        }
    }
    
    private void handleTreasury(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "This command can only be used by players!");
            return;
        }
        
        final Optional<xyz.inv1s1bl3.countries.database.entities.Player> playerDataOpt = 
            this.plugin.getCountryManager().getPlayer(player.getUniqueId());
        
        if (playerDataOpt.isEmpty() || !playerDataOpt.get().hasCountry()) {
            MessageUtil.sendMessage(sender, "country.not-in-country");
            return;
        }
        
        final xyz.inv1s1bl3.countries.database.entities.Player playerData = playerDataOpt.get();
        final Optional<Country> countryOpt = this.plugin.getCountryManager().getCountry(playerData.getCountryId());
        
        if (countryOpt.isEmpty()) {
            MessageUtil.sendMessage(sender, "general.database-error");
            return;
        }
        
        final Country country = countryOpt.get();
        
        if (args.length == 1) {
            // Show treasury balance
            MessageUtil.sendMessage(sender, "economy.balance-country", 
                Map.of(
                    "country", country.getName(),
                    "balance", this.plugin.getEconomyManager().formatMoney(country.getTreasuryBalance())
                ));
            return;
        }
        
        if (args.length >= 3) {
            final String action = args[1].toLowerCase();
            
            if (!PermissionUtil.hasCountryAdminPermissions(player)) {
                MessageUtil.sendMessage(sender, "general.no-permission");
                return;
            }
            
            try {
                final double amount = Double.parseDouble(args[2]);
                
                if (action.equals("withdraw")) {
                    if (this.plugin.getEconomyManager().transferCountryToPlayer(
                        country.getId(), player.getUniqueId(), amount, "Treasury withdrawal")) {
                        
                        MessageUtil.sendSuccess(sender, "Withdrew " + 
                            this.plugin.getEconomyManager().formatMoney(amount) + " from treasury!");
                    } else {
                        MessageUtil.sendError(sender, "Insufficient treasury funds!");
                    }
                    
                } else if (action.equals("deposit")) {
                    if (this.plugin.getEconomyManager().transferPlayerToCountry(
                        player.getUniqueId(), country.getId(), amount, "Treasury deposit")) {
                        
                        MessageUtil.sendSuccess(sender, "Deposited " + 
                            this.plugin.getEconomyManager().formatMoney(amount) + " to treasury!");
                    } else {
                        MessageUtil.sendError(sender, "Insufficient personal funds!");
                    }
                }
                
            } catch (final NumberFormatException exception) {
                MessageUtil.sendError(sender, "Invalid amount: " + args[2]);
            }
        }
    }
    
    private void handleStats(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "This command can only be used by players!");
            return;
        }
        
        if (args.length == 1) {
            // Show player's own stats
            final var stats = this.plugin.getEconomyManager().generatePlayerStats(player.getUniqueId());
            
            MessageUtil.sendInfo(sender, "=== Your Economic Statistics ===");
            MessageUtil.sendInfo(sender, "Current Balance: " + 
                this.plugin.getEconomyManager().formatMoney(stats.getCurrentBalance()));
            MessageUtil.sendInfo(sender, "Weekly Income: " + 
                this.plugin.getEconomyManager().formatMoney(stats.getWeeklyIncome()));
            MessageUtil.sendInfo(sender, "Weekly Expenses: " + 
                this.plugin.getEconomyManager().formatMoney(stats.getWeeklyExpenses()));
            MessageUtil.sendInfo(sender, "Weekly Net Income: " + 
                this.plugin.getEconomyManager().formatMoney(stats.getWeeklyNetIncome()));
            MessageUtil.sendInfo(sender, "Monthly Net Income: " + 
                this.plugin.getEconomyManager().formatMoney(stats.getMonthlyNetIncome()));
            MessageUtil.sendInfo(sender, "Total Transactions: " + stats.getTotalTransactions());
            
        } else if (args.length == 2 && args[1].equalsIgnoreCase("country")) {
            // Show country stats
            final Optional<xyz.inv1s1bl3.countries.database.entities.Player> playerDataOpt = 
                this.plugin.getCountryManager().getPlayer(player.getUniqueId());
            
            if (playerDataOpt.isEmpty() || !playerDataOpt.get().hasCountry()) {
                MessageUtil.sendMessage(sender, "country.not-in-country");
                return;
            }
            
            final xyz.inv1s1bl3.countries.database.entities.Player playerData = playerDataOpt.get();
            final var stats = this.plugin.getEconomyManager().generateCountryStats(playerData.getCountryId());
            
            if (stats != null) {
                MessageUtil.sendInfo(sender, "=== " + stats.getCountryName() + " Economic Statistics ===");
                MessageUtil.sendInfo(sender, "Treasury Balance: " + 
                    this.plugin.getEconomyManager().formatMoney(stats.getTreasuryBalance()));
                MessageUtil.sendInfo(sender, "Total Member Wealth: " + 
                    this.plugin.getEconomyManager().formatMoney(stats.getTotalMemberWealth()));
                MessageUtil.sendInfo(sender, "Member Count: " + stats.getMemberCount());
                MessageUtil.sendInfo(sender, "Average Member Wealth: " + 
                    this.plugin.getEconomyManager().formatMoney(stats.getAverageMemberWealth()));
                MessageUtil.sendInfo(sender, "Weekly Net Income: " + 
                    this.plugin.getEconomyManager().formatMoney(stats.getWeeklyNetIncome()));
                MessageUtil.sendInfo(sender, "Tax Rate: " + stats.getTaxRate() + "%");
            }
        }
    }
    
    private void handleReport(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "This command can only be used by players!");
            return;
        }
        
        if (!PermissionUtil.hasCountryAdminPermissions(player)) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }
        
        final Optional<xyz.inv1s1bl3.countries.database.entities.Player> playerDataOpt = 
            this.plugin.getCountryManager().getPlayer(player.getUniqueId());
        
        if (playerDataOpt.isEmpty() || !playerDataOpt.get().hasCountry()) {
            MessageUtil.sendMessage(sender, "country.not-in-country");
            return;
        }
        
        final xyz.inv1s1bl3.countries.database.entities.Player playerData = playerDataOpt.get();
        final var report = this.plugin.getEconomyManager().generateCountryEconomicReport(playerData.getCountryId());
        
        if (report != null) {
            MessageUtil.sendInfo(sender, "=== " + report.getCountryName() + " Economic Report ===");
            MessageUtil.sendInfo(sender, "Treasury Balance: " + 
                this.plugin.getEconomyManager().formatMoney(report.getTreasuryBalance()));
            MessageUtil.sendInfo(sender, "Member Count: " + report.getMemberCount());
            MessageUtil.sendInfo(sender, "Average Member Wealth: " + 
                this.plugin.getEconomyManager().formatMoney(report.getAverageMemberWealth()));
            MessageUtil.sendInfo(sender, "Weekly Income: " + 
                this.plugin.getEconomyManager().formatMoney(report.getWeeklyIncome()));
            MessageUtil.sendInfo(sender, "Weekly Expenses: " + 
                this.plugin.getEconomyManager().formatMoney(report.getWeeklyExpenses()));
            MessageUtil.sendInfo(sender, "Weekly Net Income: " + 
                this.plugin.getEconomyManager().formatMoney(report.getNetWeeklyIncome()));
            MessageUtil.sendInfo(sender, "Economic Health: " + report.getHealthRating() + 
                " (" + String.format("%.1f%%", report.getEconomicHealth() * 100) + ")");
            MessageUtil.sendInfo(sender, "Tax Rate: " + report.getTaxRate() + "%");
            MessageUtil.sendInfo(sender, "Territory Count: " + report.getTerritoryCount());
            MessageUtil.sendInfo(sender, "Territory Value: " + 
                this.plugin.getEconomyManager().formatMoney(report.getTerritoryValue()));
            MessageUtil.sendInfo(sender, "Daily Maintenance: " + 
                this.plugin.getEconomyManager().formatMoney(report.getDailyMaintenanceCost()));
            MessageUtil.sendInfo(sender, "Growth Rating: " + report.getGrowthRating());
            MessageUtil.sendInfo(sender, "Maintenance Coverage: " + 
                String.format("%.1f weeks", report.getMaintenanceCoverageWeeks()));
        }
    }
    
    private void handleBankruptcy(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "This command can only be used by players!");
            return;
        }
        
        if (!PermissionUtil.hasCountryAdminPermissions(player)) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }
        
        final Optional<xyz.inv1s1bl3.countries.database.entities.Player> playerDataOpt = 
            this.plugin.getCountryManager().getPlayer(player.getUniqueId());
        
        if (playerDataOpt.isEmpty() || !playerDataOpt.get().hasCountry()) {
            MessageUtil.sendMessage(sender, "country.not-in-country");
            return;
        }
        
        final xyz.inv1s1bl3.countries.database.entities.Player playerData = playerDataOpt.get();
        final var report = this.plugin.getEconomyManager().generateCountryEconomicReport(playerData.getCountryId());
        
        if (report != null) {
            MessageUtil.sendInfo(sender, "=== Bankruptcy Analysis ===");
            MessageUtil.sendInfo(sender, "Treasury Balance: " + 
                this.plugin.getEconomyManager().formatMoney(report.getTreasuryBalance()));
            MessageUtil.sendInfo(sender, "Daily Maintenance: " + 
                this.plugin.getEconomyManager().formatMoney(report.getDailyMaintenanceCost()));
            MessageUtil.sendInfo(sender, "Coverage: " + 
                String.format("%.1f weeks", report.getMaintenanceCoverageWeeks()));
            
            if (report.getMaintenanceCoverageWeeks() < 2) {
                MessageUtil.sendWarning(sender, "WARNING: Your country may face bankruptcy soon!");
                MessageUtil.sendInfo(sender, "Consider: Reducing expenses, increasing taxes, or selling territories");
            } else if (report.getMaintenanceCoverageWeeks() < 4) {
                MessageUtil.sendWarning(sender, "CAUTION: Low treasury reserves detected");
            } else {
                MessageUtil.sendSuccess(sender, "Your country's finances are stable");
            }
        }
    }
    
    private void handleMaintenance(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "This command can only be used by players!");
            return;
        }
        
        final Optional<xyz.inv1s1bl3.countries.database.entities.Player> playerDataOpt = 
            this.plugin.getCountryManager().getPlayer(player.getUniqueId());
        
        if (playerDataOpt.isEmpty() || !playerDataOpt.get().hasCountry()) {
            MessageUtil.sendMessage(sender, "country.not-in-country");
            return;
        }
        
        final xyz.inv1s1bl3.countries.database.entities.Player playerData = playerDataOpt.get();
        final List<xyz.inv1s1bl3.countries.database.entities.Territory> territories = 
            this.plugin.getTerritoryManager().getCountryTerritories(playerData.getCountryId());
        
        if (territories.isEmpty()) {
            MessageUtil.sendInfo(sender, "Your country has no territories.");
            return;
        }
        
        MessageUtil.sendInfo(sender, "=== Territory Maintenance ===");
        double totalMaintenance = 0.0;
        
        for (final xyz.inv1s1bl3.countries.database.entities.Territory territory : territories) {
            final String info = String.format("- %s (%d, %d): $%.2f/day", 
                territory.getTerritoryType(),
                territory.getChunkX(), 
                territory.getChunkZ(),
                territory.getMaintenanceCost());
            MessageUtil.sendInfo(sender, info);
            totalMaintenance += territory.getMaintenanceCost();
        }
        
        MessageUtil.sendInfo(sender, "Total Daily Maintenance: " + 
            this.plugin.getEconomyManager().formatMoney(totalMaintenance));
    }
    
    private void sendHelpMessage(final CommandSender sender) {
        MessageUtil.sendInfo(sender, "Economy Commands:");
        MessageUtil.sendInfo(sender, "/economy balance [player] - Check balance");
        MessageUtil.sendInfo(sender, "/economy transfer <player/country:name> <amount> [description] - Transfer money");
        MessageUtil.sendInfo(sender, "/economy history [limit] - View transaction history");
        MessageUtil.sendInfo(sender, "/economy tax [set <rate>|collect] - Tax management");
        MessageUtil.sendInfo(sender, "/economy salary [pay] - Salary information");
        MessageUtil.sendInfo(sender, "/economy treasury [deposit/withdraw <amount>] - Treasury management");
        MessageUtil.sendInfo(sender, "/economy stats [country] - Economic statistics");
        MessageUtil.sendInfo(sender, "/economy report - Country economic report (admin only)");
        MessageUtil.sendInfo(sender, "/economy bankruptcy - Bankruptcy analysis (admin only)");
        MessageUtil.sendInfo(sender, "/economy maintenance - Territory maintenance costs");
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final Command command, 
                                               @NotNull final String alias, @NotNull final String[] args) {
        
        if (args.length == 1) {
            return Arrays.asList("balance", "transfer", "history", "tax", "salary", "treasury", "stats", "report", "bankruptcy", "maintenance", "help")
                .stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "balance" -> {
                    return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                }
                case "transfer" -> {
                    final List<String> suggestions = new ArrayList<>();
                    
                    // Add online players
                    suggestions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList());
                    
                    // Add countries with "country:" prefix
                    suggestions.addAll(this.plugin.getCountryManager().getAllActiveCountries().stream()
                        .map(country -> "country:" + country.getName())
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList());
                    
                    return suggestions;
                }
                case "tax" -> {
                    return Arrays.asList("set", "collect")
                        .stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                }
                case "salary" -> {
                    return Arrays.asList("pay")
                        .stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                }
                case "treasury" -> {
                    return Arrays.asList("deposit", "withdraw")
                        .stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                }
                case "stats" -> {
                    return Arrays.asList("country")
                        .stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                }
            }
        }
        
        return new ArrayList<>();
    }
}