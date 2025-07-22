package xyz.inv1s1bl3.countries.commands.admin;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Administrative commands for Countries plugin
 */
@RequiredArgsConstructor
public final class AdminCommand implements CommandExecutor, TabCompleter {
    
    private final CountriesPlugin plugin;
    
    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, 
                           @NotNull final String label, @NotNull final String[] args) {
        
        if (!sender.hasPermission("countries.admin")) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return true;
        }
        
        if (args.length == 0) {
            this.sendHelpMessage(sender);
            return true;
        }
        
        final String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload" -> this.handleReload(sender);
            case "info" -> this.handleInfo(sender, args);
            case "economy" -> this.handleEconomy(sender, args);
            case "country" -> this.handleCountry(sender, args);
            case "territory" -> this.handleTerritory(sender, args);
            case "player" -> this.handlePlayer(sender, args);
            case "stats" -> this.handleStats(sender);
            case "audit" -> this.handleAudit(sender, args);
            case "help" -> this.sendHelpMessage(sender);
            default -> MessageUtil.sendMessage(sender, "general.invalid-command", 
                    Map.of("usage", "/countries-admin help"));
        }
        
        return true;
    }
    
    private void handleReload(final CommandSender sender) {
        try {
            this.plugin.reload();
            MessageUtil.sendMessage(sender, "general.reload-success");
        } catch (final Exception exception) {
            MessageUtil.sendMessage(sender, "general.reload-error", 
                Map.of("error", exception.getMessage()));
        }
    }
    
    private void handleInfo(final CommandSender sender, final String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(sender, "Usage: /countries-admin info <country>");
            return;
        }
        
        final String countryName = args[1];
        final Optional<Country> countryOpt = this.plugin.getCountryManager().getCountryByName(countryName);
        
        if (countryOpt.isEmpty()) {
            MessageUtil.sendError(sender, "Country '" + countryName + "' not found!");
            return;
        }
        
        final Country country = countryOpt.get();
        final List<xyz.inv1s1bl3.countries.database.entities.Player> members = 
            this.plugin.getCountryManager().getCountryMembers(country.getId());
        
        MessageUtil.sendInfo(sender, "=== Country Information ===");
        MessageUtil.sendInfo(sender, "Name: " + country.getName());
        MessageUtil.sendInfo(sender, "Display Name: " + country.getDisplayName());
        MessageUtil.sendInfo(sender, "Government: " + country.getGovernmentType());
        MessageUtil.sendInfo(sender, "Leader: " + Bukkit.getOfflinePlayer(country.getLeaderUuid()).getName());
        MessageUtil.sendInfo(sender, "Members: " + members.size());
        MessageUtil.sendInfo(sender, "Treasury: " + this.plugin.getEconomyManager().formatMoney(country.getTreasuryBalance()));
        MessageUtil.sendInfo(sender, "Tax Rate: " + country.getTaxRate() + "%");
        MessageUtil.sendInfo(sender, "Active: " + (country.isActive() ? "Yes" : "No"));
        MessageUtil.sendInfo(sender, "Created: " + country.getCreatedAt().toLocalDate());
        
        if (country.hasCapital()) {
            MessageUtil.sendInfo(sender, "Capital: " + country.getCapitalWorld() + " (" + 
                country.getCapitalX() + ", " + country.getCapitalZ() + ")");
        }
    }
    
    private void handleEconomy(final CommandSender sender, final String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(sender, "Usage: /countries-admin economy <subcommand>");
            return;
        }
        
        final String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "give" -> this.handleEconomyGive(sender, args);
            case "take" -> this.handleEconomyTake(sender, args);
            case "set" -> this.handleEconomySet(sender, args);
            case "treasury" -> this.handleEconomyTreasury(sender, args);
            case "tax-collect" -> this.handleTaxCollect(sender, args);
            case "salary-pay" -> this.handleSalaryPay(sender, args);
            case "transactions" -> this.handleTransactions(sender, args);
            default -> MessageUtil.sendError(sender, "Invalid economy subcommand!");
        }
    }
    
    private void handleEconomyGive(final CommandSender sender, final String[] args) {
        if (args.length < 4) {
            MessageUtil.sendError(sender, "Usage: /countries-admin economy give <player> <amount>");
            return;
        }
        
        final String playerName = args[2];
        final OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        
        if (!target.hasPlayedBefore()) {
            MessageUtil.sendError(sender, "Player '" + playerName + "' not found!");
            return;
        }
        
        try {
            final double amount = Double.parseDouble(args[3]);
            
            if (this.plugin.getEconomyManager().getVaultIntegration().depositPlayer(target, amount)) {
                MessageUtil.sendSuccess(sender, "Gave " + 
                    this.plugin.getEconomyManager().formatMoney(amount) + " to " + target.getName());
                
                // Record admin transaction
                this.plugin.getEconomyManager().getTransactionManager().recordPlayerToPlayerTransaction(
                    null, target.getUniqueId(), amount, "Admin give command", "admin", null
                );
            } else {
                MessageUtil.sendError(sender, "Failed to give money to player!");
            }
            
        } catch (final NumberFormatException exception) {
            MessageUtil.sendError(sender, "Invalid amount: " + args[3]);
        }
    }
    
    private void handleEconomyTake(final CommandSender sender, final String[] args) {
        if (args.length < 4) {
            MessageUtil.sendError(sender, "Usage: /countries-admin economy take <player> <amount>");
            return;
        }
        
        final String playerName = args[2];
        final OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        
        if (!target.hasPlayedBefore()) {
            MessageUtil.sendError(sender, "Player '" + playerName + "' not found!");
            return;
        }
        
        try {
            final double amount = Double.parseDouble(args[3]);
            
            if (this.plugin.getEconomyManager().getVaultIntegration().withdrawPlayer(target, amount)) {
                MessageUtil.sendSuccess(sender, "Took " + 
                    this.plugin.getEconomyManager().formatMoney(amount) + " from " + target.getName());
                
                // Record admin transaction
                this.plugin.getEconomyManager().getTransactionManager().recordPlayerToPlayerTransaction(
                    target.getUniqueId(), null, amount, "Admin take command", "admin", null
                );
            } else {
                MessageUtil.sendError(sender, "Failed to take money from player!");
            }
            
        } catch (final NumberFormatException exception) {
            MessageUtil.sendError(sender, "Invalid amount: " + args[3]);
        }
    }
    
    private void handleEconomySet(final CommandSender sender, final String[] args) {
        if (args.length < 4) {
            MessageUtil.sendError(sender, "Usage: /countries-admin economy set <player> <amount>");
            return;
        }
        
        final String playerName = args[2];
        final OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        
        if (!target.hasPlayedBefore()) {
            MessageUtil.sendError(sender, "Player '" + playerName + "' not found!");
            return;
        }
        
        try {
            final double amount = Double.parseDouble(args[3]);
            final double currentBalance = this.plugin.getEconomyManager().getPlayerBalance(target.getUniqueId());
            final double difference = amount - currentBalance;
            
            if (difference > 0) {
                this.plugin.getEconomyManager().getVaultIntegration().depositPlayer(target, difference);
            } else if (difference < 0) {
                this.plugin.getEconomyManager().getVaultIntegration().withdrawPlayer(target, Math.abs(difference));
            }
            
            MessageUtil.sendSuccess(sender, "Set " + target.getName() + "'s balance to " + 
                this.plugin.getEconomyManager().formatMoney(amount));
            
        } catch (final NumberFormatException exception) {
            MessageUtil.sendError(sender, "Invalid amount: " + args[3]);
        }
    }
    
    private void handleEconomyTreasury(final CommandSender sender, final String[] args) {
        if (args.length < 5) {
            MessageUtil.sendError(sender, "Usage: /countries-admin economy treasury <country> <add/remove/set> <amount>");
            return;
        }
        
        final String countryName = args[2];
        final String action = args[3].toLowerCase();
        
        final Optional<Country> countryOpt = this.plugin.getCountryManager().getCountryByName(countryName);
        if (countryOpt.isEmpty()) {
            MessageUtil.sendError(sender, "Country '" + countryName + "' not found!");
            return;
        }
        
        final Country country = countryOpt.get();
        
        try {
            final double amount = Double.parseDouble(args[4]);
            
            switch (action) {
                case "add" -> {
                    country.addToTreasury(amount);
                    MessageUtil.sendSuccess(sender, "Added " + 
                        this.plugin.getEconomyManager().formatMoney(amount) + " to " + country.getName() + "'s treasury");
                }
                case "remove" -> {
                    if (country.removeFromTreasury(amount)) {
                        MessageUtil.sendSuccess(sender, "Removed " + 
                            this.plugin.getEconomyManager().formatMoney(amount) + " from " + country.getName() + "'s treasury");
                    } else {
                        MessageUtil.sendError(sender, "Insufficient treasury funds!");
                    }
                }
                case "set" -> {
                    country.setTreasuryBalance(amount);
                    MessageUtil.sendSuccess(sender, "Set " + country.getName() + "'s treasury to " + 
                        this.plugin.getEconomyManager().formatMoney(amount));
                }
                default -> MessageUtil.sendError(sender, "Invalid action: " + action);
            }
            
            this.plugin.getCountryManager().updateCountry(country);
            
        } catch (final NumberFormatException exception) {
            MessageUtil.sendError(sender, "Invalid amount: " + args[4]);
        }
    }
    
    private void handleTaxCollect(final CommandSender sender, final String[] args) {
        if (args.length == 2) {
            // Collect all taxes
            this.plugin.getEconomyManager().getTaxManager().collectAllTaxes();
            MessageUtil.sendSuccess(sender, "Tax collection initiated for all countries!");
            
        } else if (args.length == 3) {
            // Collect taxes for specific country
            final String countryName = args[2];
            final Optional<Country> countryOpt = this.plugin.getCountryManager().getCountryByName(countryName);
            
            if (countryOpt.isEmpty()) {
                MessageUtil.sendError(sender, "Country '" + countryName + "' not found!");
                return;
            }
            
            final int collected = this.plugin.getEconomyManager().getTaxManager().collectCountryTaxes(countryOpt.get());
            MessageUtil.sendSuccess(sender, "Collected taxes from " + collected + " citizens of " + countryName);
        }
    }
    
    private void handleSalaryPay(final CommandSender sender, final String[] args) {
        if (args.length == 2) {
            // Pay all salaries
            this.plugin.getEconomyManager().getTaxManager().payAllSalaries();
            MessageUtil.sendSuccess(sender, "Salary payments initiated for all countries!");
            
        } else if (args.length == 3) {
            // Pay salaries for specific country
            final String countryName = args[2];
            final Optional<Country> countryOpt = this.plugin.getCountryManager().getCountryByName(countryName);
            
            if (countryOpt.isEmpty()) {
                MessageUtil.sendError(sender, "Country '" + countryName + "' not found!");
                return;
            }
            
            final int paid = this.plugin.getEconomyManager().getTaxManager().payCountrySalaries(countryOpt.get());
            MessageUtil.sendSuccess(sender, "Paid salaries to " + paid + " members of " + countryName);
        }
    }
    
    private void handleTransactions(final CommandSender sender, final String[] args) {
        final int limit = args.length > 2 ? Math.min(Integer.parseInt(args[2]), 50) : 20;
        final List<Transaction> transactions = this.plugin.getEconomyManager()
            .getTransactionManager().getRecentTransactions(limit);
        
        if (transactions.isEmpty()) {
            MessageUtil.sendInfo(sender, "No recent transactions found.");
            return;
        }
        
        MessageUtil.sendInfo(sender, "Recent Transactions (" + transactions.size() + "):");
        for (final Transaction transaction : transactions) {
            MessageUtil.sendInfo(sender, String.format("&7[%s] &e%s &7- &f%s &7(%s)", 
                transaction.getTransactionType().toUpperCase(),
                this.plugin.getEconomyManager().formatMoney(transaction.getAmount()),
                transaction.getDescription(),
                transaction.getCreatedAt().toLocalDate()));
        }
    }
    
    private void handleCountry(final CommandSender sender, final String[] args) {
        if (args.length < 3) {
            MessageUtil.sendError(sender, "Usage: /countries-admin country <create/delete/transfer> ...");
            return;
        }
        
        final String action = args[1].toLowerCase();
        
        switch (action) {
            case "delete" -> this.handleCountryDelete(sender, args);
            case "transfer" -> this.handleCountryTransfer(sender, args);
            default -> MessageUtil.sendError(sender, "Invalid country action: " + action);
        }
    }
    
    private void handleCountryDelete(final CommandSender sender, final String[] args) {
        if (args.length < 3) {
            MessageUtil.sendError(sender, "Usage: /countries-admin country delete <country>");
            return;
        }
        
        final String countryName = args[2];
        final Optional<Country> countryOpt = this.plugin.getCountryManager().getCountryByName(countryName);
        
        if (countryOpt.isEmpty()) {
            MessageUtil.sendError(sender, "Country '" + countryName + "' not found!");
            return;
        }
        
        final Country country = countryOpt.get();
        
        try {
            this.plugin.getCountryManager().dissolveCountry(country.getId(), null); // Admin dissolution
            MessageUtil.sendSuccess(sender, "Country '" + countryName + "' has been deleted!");
            
        } catch (final Exception exception) {
            MessageUtil.sendError(sender, "Failed to delete country: " + exception.getMessage());
        }
    }
    
    private void handleCountryTransfer(final CommandSender sender, final String[] args) {
        if (args.length < 4) {
            MessageUtil.sendError(sender, "Usage: /countries-admin country transfer <country> <new-leader>");
            return;
        }
        
        final String countryName = args[2];
        final String newLeaderName = args[3];
        
        final Optional<Country> countryOpt = this.plugin.getCountryManager().getCountryByName(countryName);
        if (countryOpt.isEmpty()) {
            MessageUtil.sendError(sender, "Country '" + countryName + "' not found!");
            return;
        }
        
        final OfflinePlayer newLeader = Bukkit.getOfflinePlayer(newLeaderName);
        if (!newLeader.hasPlayedBefore()) {
            MessageUtil.sendError(sender, "Player '" + newLeaderName + "' not found!");
            return;
        }
        
        final Country country = countryOpt.get();
        
        try {
            this.plugin.getCountryManager().transferLeadership(
                country.getId(), country.getLeaderUuid(), newLeader.getUniqueId()
            );
            
            MessageUtil.sendSuccess(sender, "Transferred leadership of " + countryName + " to " + newLeaderName);
            
        } catch (final Exception exception) {
            MessageUtil.sendError(sender, "Failed to transfer leadership: " + exception.getMessage());
        }
    }
    
    private void handleTerritory(final CommandSender sender, final String[] args) {
        MessageUtil.sendError(sender, "Territory admin commands not yet implemented!");
    }
    
    private void handlePlayer(final CommandSender sender, final String[] args) {
        if (args.length < 3) {
            MessageUtil.sendError(sender, "Usage: /countries-admin player <player> <info/reset>");
            return;
        }
        
        final String playerName = args[1];
        final String action = args[2].toLowerCase();
        
        final OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (!target.hasPlayedBefore()) {
            MessageUtil.sendError(sender, "Player '" + playerName + "' not found!");
            return;
        }
        
        final Optional<xyz.inv1s1bl3.countries.database.entities.Player> playerDataOpt = 
            this.plugin.getCountryManager().getPlayer(target.getUniqueId());
        
        if (playerDataOpt.isEmpty()) {
            MessageUtil.sendError(sender, "Player data not found!");
            return;
        }
        
        final xyz.inv1s1bl3.countries.database.entities.Player playerData = playerDataOpt.get();
        
        if (action.equals("info")) {
            MessageUtil.sendInfo(sender, "=== Player Information ===");
            MessageUtil.sendInfo(sender, "Name: " + playerData.getUsername());
            MessageUtil.sendInfo(sender, "UUID: " + playerData.getUuid());
            MessageUtil.sendInfo(sender, "Country: " + (playerData.hasCountry() ? 
                this.plugin.getCountryManager().getCountry(playerData.getCountryId())
                    .map(Country::getName).orElse("Unknown") : "None"));
            MessageUtil.sendInfo(sender, "Role: " + playerData.getRole());
            MessageUtil.sendInfo(sender, "Balance: " + this.plugin.getEconomyManager().formatMoney(playerData.getBalance()));
            MessageUtil.sendInfo(sender, "Online: " + (playerData.isOnline() ? "Yes" : "No"));
            MessageUtil.sendInfo(sender, "Last Seen: " + playerData.getLastSeen().toLocalDate());
            MessageUtil.sendInfo(sender, "Joined: " + playerData.getJoinedAt().toLocalDate());
        }
    }
    
    private void handleStats(final CommandSender sender) {
        final List<Country> countries = this.plugin.getCountryManager().getAllActiveCountries();
        final int totalPlayers = Bukkit.getOfflinePlayers().length;
        
        MessageUtil.sendInfo(sender, "=== Countries Plugin Statistics ===");
        MessageUtil.sendInfo(sender, "Active Countries: " + countries.size());
        MessageUtil.sendInfo(sender, "Total Players: " + totalPlayers);
        MessageUtil.sendInfo(sender, "Online Players: " + Bukkit.getOnlinePlayers().size());
        
        if (this.plugin.getEconomyManager().getVaultIntegration().isAvailable()) {
            MessageUtil.sendInfo(sender, "Economy: Vault Integration Active");
        } else {
            MessageUtil.sendInfo(sender, "Economy: No Vault Integration");
        }
        
        // Database stats
        try {
            final long transactionCount = this.plugin.getEconomyManager().getTransactionManager()
                .getRecentTransactions(1).size() > 0 ? 
                this.plugin.getEconomyManager().getTransactionRepository().count() : 0;
            MessageUtil.sendInfo(sender, "Total Transactions: " + transactionCount);
        } catch (final Exception exception) {
            MessageUtil.sendInfo(sender, "Transaction Count: Error retrieving");
        }
    }
    
    private void handleAudit(final CommandSender sender, final String[] args) {
        if (args.length == 1) {
            // Perform full audit
            MessageUtil.sendInfo(sender, "Starting comprehensive economic audit...");
            
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                final var report = this.plugin.getEconomyManager().performEconomicAudit();
                
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    MessageUtil.sendInfo(sender, "=== Economic Audit Report ===");
                    MessageUtil.sendInfo(sender, "Status: " + report.getStatus());
                    MessageUtil.sendInfo(sender, "Duration: " + report.getAuditDurationMinutes() + " minutes");
                    MessageUtil.sendInfo(sender, "Issues Found: " + report.getIssues().size());
                    MessageUtil.sendInfo(sender, "Warnings: " + report.getWarnings().size());
                    
                    if (report.hasIssues()) {
                        MessageUtil.sendInfo(sender, "");
                        MessageUtil.sendInfo(sender, "Critical Issues:");
                        for (int i = 0; i < Math.min(5, report.getIssues().size()); i++) {
                            MessageUtil.sendError(sender, "- " + report.getIssues().get(i));
                        }
                        if (report.getIssues().size() > 5) {
                            MessageUtil.sendInfo(sender, "... and " + (report.getIssues().size() - 5) + " more issues");
                        }
                    }
                    
                    if (report.hasWarnings()) {
                        MessageUtil.sendInfo(sender, "");
                        MessageUtil.sendInfo(sender, "Warnings:");
                        for (int i = 0; i < Math.min(3, report.getWarnings().size()); i++) {
                            MessageUtil.sendWarning(sender, "- " + report.getWarnings().get(i));
                        }
                        if (report.getWarnings().size() > 3) {
                            MessageUtil.sendInfo(sender, "... and " + (report.getWarnings().size() - 3) + " more warnings");
                        }
                    }
                });
            });
            
        } else if (args.length == 2) {
            final String auditType = args[1].toLowerCase();
            
            switch (auditType) {
                case "stats" -> {
                    final var globalStats = this.plugin.getEconomyManager().generateGlobalStats();
                    
                    MessageUtil.sendInfo(sender, "=== Global Economic Statistics ===");
                    MessageUtil.sendInfo(sender, "Total Countries: " + globalStats.getTotalCountries());
                    MessageUtil.sendInfo(sender, "Total Treasury Balance: " + 
                        this.plugin.getEconomyManager().formatMoney(globalStats.getTotalTreasuryBalance()));
                    MessageUtil.sendInfo(sender, "Total Player Wealth: " + 
                        this.plugin.getEconomyManager().formatMoney(globalStats.getTotalPlayerWealth()));
                    MessageUtil.sendInfo(sender, "Total Economic Value: " + 
                        this.plugin.getEconomyManager().formatMoney(globalStats.getTotalEconomicValue()));
                    MessageUtil.sendInfo(sender, "Total Transactions: " + globalStats.getTotalTransactions());
                    MessageUtil.sendInfo(sender, "Average Treasury Balance: " + 
                        this.plugin.getEconomyManager().formatMoney(globalStats.getAverageTreasuryBalance()));
                }
                case "country" -> {
                    if (args.length < 3) {
                        MessageUtil.sendError(sender, "Usage: /countries-admin audit country <country-name>");
                        return;
                    }
                    
                    final String countryName = args[2];
                    final var countryOpt = this.plugin.getCountryManager().getCountryByName(countryName);
                    
                    if (countryOpt.isEmpty()) {
                        MessageUtil.sendError(sender, "Country '" + countryName + "' not found!");
                        return;
                    }
                    
                    final var country = countryOpt.get();
                    final var stats = this.plugin.getEconomyManager().generateCountryStats(country.getId());
                    
                    if (stats != null) {
                        MessageUtil.sendInfo(sender, "=== " + stats.getCountryName() + " Economic Statistics ===");
                        MessageUtil.sendInfo(sender, "Treasury Balance: " + 
                            this.plugin.getEconomyManager().formatMoney(stats.getTreasuryBalance()));
                        MessageUtil.sendInfo(sender, "Total Member Wealth: " + 
                            this.plugin.getEconomyManager().formatMoney(stats.getTotalMemberWealth()));
                        MessageUtil.sendInfo(sender, "Member Count: " + stats.getMemberCount());
                        MessageUtil.sendInfo(sender, "Average Member Wealth: " + 
                            this.plugin.getEconomyManager().formatMoney(stats.getAverageMemberWealth()));
                        MessageUtil.sendInfo(sender, "Weekly Income: " + 
                            this.plugin.getEconomyManager().formatMoney(stats.getWeeklyIncome()));
                        MessageUtil.sendInfo(sender, "Weekly Expenses: " + 
                            this.plugin.getEconomyManager().formatMoney(stats.getWeeklyExpenses()));
                        MessageUtil.sendInfo(sender, "Weekly Net Income: " + 
                            this.plugin.getEconomyManager().formatMoney(stats.getWeeklyNetIncome()));
                        MessageUtil.sendInfo(sender, "Tax Rate: " + stats.getTaxRate() + "%");
                        MessageUtil.sendInfo(sender, "Territory Count: " + stats.getTerritoryCount());
                        MessageUtil.sendInfo(sender, "Territory Value: " + 
                            this.plugin.getEconomyManager().formatMoney(stats.getTerritoryValue()));
                        MessageUtil.sendInfo(sender, "Daily Maintenance: " + 
                            this.plugin.getEconomyManager().formatMoney(stats.getDailyMaintenanceCost()));
                    }
                }
                default -> MessageUtil.sendError(sender, "Invalid audit type: " + auditType);
            }
        }
    }
    
    private void sendHelpMessage(final CommandSender sender) {
        MessageUtil.sendInfo(sender, "Countries Admin Commands:");
        MessageUtil.sendInfo(sender, "/countries-admin reload - Reload plugin");
        MessageUtil.sendInfo(sender, "/countries-admin info <country> - Country information");
        MessageUtil.sendInfo(sender, "/countries-admin economy <subcommand> - Economy management");
        MessageUtil.sendInfo(sender, "/countries-admin country <action> - Country management");
        MessageUtil.sendInfo(sender, "/countries-admin player <player> <action> - Player management");
        MessageUtil.sendInfo(sender, "/countries-admin stats - Plugin statistics");
        MessageUtil.sendInfo(sender, "/countries-admin audit [type] - Economic auditing");
        MessageUtil.sendInfo(sender, "");
        MessageUtil.sendInfo(sender, "Economy subcommands:");
        MessageUtil.sendInfo(sender, "  give/take/set <player> <amount> - Manage player money");
        MessageUtil.sendInfo(sender, "  treasury <country> <add/remove/set> <amount> - Manage treasury");
        MessageUtil.sendInfo(sender, "  tax-collect [country] - Force tax collection");
        MessageUtil.sendInfo(sender, "  salary-pay [country] - Force salary payments");
        MessageUtil.sendInfo(sender, "  transactions [limit] - View recent transactions");
        MessageUtil.sendInfo(sender, "");
        MessageUtil.sendInfo(sender, "Audit types:");
        MessageUtil.sendInfo(sender, "  stats - Global economic statistics");
        MessageUtil.sendInfo(sender, "  country <name> - Country economic analysis");
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final Command command, 
                                               @NotNull final String alias, @NotNull final String[] args) {
        
        if (!sender.hasPermission("countries.admin")) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            return Arrays.asList("reload", "info", "economy", "country", "territory", "player", "stats", "audit", "help")
                .stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "info" -> {
                    return this.plugin.getCountryManager().getAllActiveCountries().stream()
                        .map(Country::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                }
                case "economy" -> {
                    return Arrays.asList("give", "take", "set", "treasury", "tax-collect", "salary-pay", "transactions")
                        .stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                }
                case "country" -> {
                    return Arrays.asList("delete", "transfer")
                        .stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                }
                case "player" -> {
                    return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                }
                case "audit" -> {
                    return Arrays.asList("stats", "country")
                        .stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                }
            }
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("audit") && args[1].equalsIgnoreCase("country")) {
            return this.plugin.getCountryManager().getAllActiveCountries().stream()
                .map(xyz.inv1s1bl3.countries.database.entities.Country::getName)
                .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}