package xyz.inv1s1bl3.countries.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.country.Country;
import xyz.inv1s1bl3.countries.core.diplomacy.DiplomaticRelation;
import xyz.inv1s1bl3.countries.core.diplomacy.TradeAgreement;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.*;

/**
 * Handles all diplomacy-related commands.
 */
public class DiplomacyCommand implements CommandExecutor, TabCompleter {
    
    private final CountriesPlugin plugin;
    
    public DiplomacyCommand(CountriesPlugin plugin) {
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
            case "ally" -> handleAlly(sender, args);
            case "enemy", "war" -> handleWar(sender, args);
            case "neutral" -> handleNeutral(sender, args);
            case "trade" -> handleTrade(sender, args);
            case "info" -> handleInfo(sender, args);
            case "list" -> handleList(sender, args);
            case "accept" -> handleAccept(sender, args);
            case "reject" -> handleReject(sender, args);
            case "gui" -> handleGUI(sender, args);
            case "help" -> sendHelp(sender);
            default -> {
                ChatUtils.sendError(sender, "Unknown subcommand. Use /diplomacy help for available commands.");
                sendHelp(sender);
            }
        }
        
        return true;
    }
    
    private void handleAlly(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can propose alliances!");
            return;
        }
        
        if (!sender.hasPermission("countries.diplomacy.ally")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(sender, "Usage: /diplomacy ally <country>");
            return;
        }
        
        String targetCountry = args[1];
        
        Country playerCountry = plugin.getCountryManager().getPlayerCountry(player);
        if (playerCountry == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-member");
            return;
        }
        
        Country target = plugin.getCountryManager().getCountry(targetCountry);
        if (target == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-found", targetCountry);
            return;
        }
        
        if (playerCountry.getName().equalsIgnoreCase(targetCountry)) {
            ChatUtils.sendError(sender, "You cannot form an alliance with your own country!");
            return;
        }
        
        if (plugin.getDiplomacyManager().proposeAlliance(player.getUniqueId(), 
                playerCountry.getName(), targetCountry)) {
            ChatUtils.sendPrefixedConfigMessage(sender, "diplomacy.alliance-proposed", targetCountry);
        } else {
            ChatUtils.sendError(sender, "Failed to propose alliance. Check requirements and try again.");
        }
    }
    
    private void handleWar(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can declare war!");
            return;
        }
        
        if (!sender.hasPermission("countries.diplomacy.enemy")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(sender, "Usage: /diplomacy war <country> [reason]");
            return;
        }
        
        String targetCountry = args[1];
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : null;
        
        Country playerCountry = plugin.getCountryManager().getPlayerCountry(player);
        if (playerCountry == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-member");
            return;
        }
        
        Country target = plugin.getCountryManager().getCountry(targetCountry);
        if (target == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-found", targetCountry);
            return;
        }
        
        if (playerCountry.getName().equalsIgnoreCase(targetCountry)) {
            ChatUtils.sendError(sender, "You cannot declare war on your own country!");
            return;
        }
        
        if (plugin.getDiplomacyManager().declareWar(player.getUniqueId(), 
                playerCountry.getName(), targetCountry, reason)) {
            ChatUtils.sendPrefixedConfigMessage(sender, "diplomacy.war-declared", targetCountry);
        } else {
            ChatUtils.sendError(sender, "Failed to declare war. Check requirements and try again.");
        }
    }
    
    private void handleNeutral(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can set neutral relations!");
            return;
        }
        
        if (!sender.hasPermission("countries.diplomacy.neutral")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(sender, "Usage: /diplomacy neutral <country>");
            return;
        }
        
        String targetCountry = args[1];
        
        Country playerCountry = plugin.getCountryManager().getPlayerCountry(player);
        if (playerCountry == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-member");
            return;
        }
        
        Country target = plugin.getCountryManager().getCountry(targetCountry);
        if (target == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-found", targetCountry);
            return;
        }
        
        if (plugin.getDiplomacyManager().setNeutral(player.getUniqueId(), 
                playerCountry.getName(), targetCountry)) {
            ChatUtils.sendPrefixedConfigMessage(sender, "diplomacy.neutral-set", targetCountry);
        } else {
            ChatUtils.sendError(sender, "Failed to set neutral relations.");
        }
    }
    
    private void handleTrade(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can propose trade agreements!");
            return;
        }
        
        if (!sender.hasPermission("countries.diplomacy.trade")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(sender, "Usage: /diplomacy trade <country> [tax_rate] [duration_days]");
            return;
        }
        
        String targetCountry = args[1];
        double taxRate = 0.05; // Default 5%
        long durationDays = 30; // Default 30 days
        
        if (args.length > 2) {
            try {
                taxRate = Double.parseDouble(args[2]);
                if (taxRate < 0 || taxRate > 1) {
                    ChatUtils.sendError(sender, "Tax rate must be between 0.0 and 1.0!");
                    return;
                }
            } catch (NumberFormatException e) {
                ChatUtils.sendError(sender, "Invalid tax rate!");
                return;
            }
        }
        
        if (args.length > 3) {
            try {
                durationDays = Long.parseLong(args[3]);
                if (durationDays <= 0) {
                    ChatUtils.sendError(sender, "Duration must be positive!");
                    return;
                }
            } catch (NumberFormatException e) {
                ChatUtils.sendError(sender, "Invalid duration!");
                return;
            }
        }
        
        Country playerCountry = plugin.getCountryManager().getPlayerCountry(player);
        if (playerCountry == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-member");
            return;
        }
        
        Country target = plugin.getCountryManager().getCountry(targetCountry);
        if (target == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-found", targetCountry);
            return;
        }
        
        if (plugin.getDiplomacyManager().proposeTradeAgreement(player.getUniqueId(), 
                playerCountry.getName(), targetCountry, taxRate, durationDays)) {
            ChatUtils.sendSuccess(sender, "Trade agreement proposed to " + targetCountry + "!");
            ChatUtils.sendInfo(sender, "Tax Rate: " + ChatUtils.formatPercentage(taxRate) + 
                    ", Duration: " + durationDays + " days");
        } else {
            ChatUtils.sendError(sender, "Failed to propose trade agreement.");
        }
    }
    
    private void handleInfo(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can view diplomatic info!");
            return;
        }
        
        if (!sender.hasPermission("countries.diplomacy.info")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        Country playerCountry = plugin.getCountryManager().getPlayerCountry(player);
        if (playerCountry == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-member");
            return;
        }
        
        if (args.length >= 2) {
            // Show specific relation info
            String targetCountry = args[1];
            DiplomaticRelation relation = plugin.getDiplomacyManager()
                    .getRelation(playerCountry.getName(), targetCountry);
            
            if (relation == null) {
                ChatUtils.sendError(sender, "No diplomatic relation with " + targetCountry + "!");
                return;
            }
            
            for (String line : relation.getFormattedInfo()) {
                sender.sendMessage(line);
            }
        } else {
            // Show all relations
            Set<DiplomaticRelation> relations = plugin.getDiplomacyManager()
                    .getCountryRelations(playerCountry.getName());
            
            if (relations.isEmpty()) {
                ChatUtils.sendInfo(sender, "Your country has no diplomatic relations.");
                return;
            }
            
            sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            sender.sendMessage(ChatUtils.colorize("&6&lDiplomatic Relations"));
            sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            
            for (DiplomaticRelation relation : relations) {
                String otherCountry = relation.getOtherCountry(playerCountry.getName());
                String status = relation.isActive() ? "&a●" : "&7●";
                sender.sendMessage(ChatUtils.colorize(status + " &e" + otherCountry + 
                        " &7(" + relation.getRelationType().getDisplayName() + ")"));
            }
            
            sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        }
    }
    
    private void handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("countries.diplomacy.info")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        String listType = args.length > 1 ? args[1].toLowerCase() : "all";
        
        switch (listType) {
            case "allies" -> listAllies(sender);
            case "enemies", "wars" -> listEnemies(sender);
            case "trade" -> listTradeAgreements(sender);
            default -> listAllRelations(sender);
        }
    }
    
    private void handleAccept(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can accept proposals!");
            return;
        }
        
        if (args.length < 3) {
            ChatUtils.sendError(sender, "Usage: /diplomacy accept <alliance|trade> <country>");
            return;
        }
        
        String proposalType = args[1].toLowerCase();
        String proposerCountry = args[2];
        
        Country playerCountry = plugin.getCountryManager().getPlayerCountry(player);
        if (playerCountry == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-member");
            return;
        }
        
        switch (proposalType) {
            case "alliance" -> {
                if (plugin.getDiplomacyManager().acceptAlliance(player.getUniqueId(), 
                        playerCountry.getName(), proposerCountry)) {
                    ChatUtils.sendSuccess(sender, "Alliance with " + proposerCountry + " accepted!");
                } else {
                    ChatUtils.sendError(sender, "Failed to accept alliance proposal.");
                }
            }
            case "trade" -> {
                // Trade agreement acceptance would be implemented here
                ChatUtils.sendError(sender, "Trade agreement acceptance not yet implemented.");
            }
            default -> ChatUtils.sendError(sender, "Invalid proposal type! Use 'alliance' or 'trade'.");
        }
    }
    
    private void handleReject(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can reject proposals!");
            return;
        }
        
        // Proposal rejection would be implemented here
        ChatUtils.sendError(sender, "Proposal rejection not yet implemented.");
    }
    
    private void listAllies(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can view allies!");
            return;
        }
        
        Country playerCountry = plugin.getCountryManager().getPlayerCountry(player);
        if (playerCountry == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-member");
            return;
        }
        
        Set<String> allies = plugin.getDiplomacyManager().getAllies(playerCountry.getName());
        
        if (allies.isEmpty()) {
            ChatUtils.sendInfo(sender, "Your country has no allies.");
            return;
        }
        
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ChatUtils.colorize("&6&lAllies (" + allies.size() + ")"));
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        for (String ally : allies) {
            sender.sendMessage(ChatUtils.colorize("&a● &e" + ally));
        }
        
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    private void listEnemies(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can view enemies!");
            return;
        }
        
        Country playerCountry = plugin.getCountryManager().getPlayerCountry(player);
        if (playerCountry == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-member");
            return;
        }
        
        Set<String> enemies = plugin.getDiplomacyManager().getEnemies(playerCountry.getName());
        
        if (enemies.isEmpty()) {
            ChatUtils.sendInfo(sender, "Your country is not at war with anyone.");
            return;
        }
        
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ChatUtils.colorize("&c&lEnemies (" + enemies.size() + ")"));
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        for (String enemy : enemies) {
            sender.sendMessage(ChatUtils.colorize("&c● &e" + enemy));
        }
        
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    private void listTradeAgreements(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can view trade agreements!");
            return;
        }
        
        Country playerCountry = plugin.getCountryManager().getPlayerCountry(player);
        if (playerCountry == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-member");
            return;
        }
        
        Set<TradeAgreement> agreements = plugin.getDiplomacyManager()
                .getCountryTradeAgreements(playerCountry.getName());
        
        if (agreements.isEmpty()) {
            ChatUtils.sendInfo(sender, "Your country has no trade agreements.");
            return;
        }
        
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ChatUtils.colorize("&6&lTrade Agreements (" + agreements.size() + ")"));
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        for (TradeAgreement agreement : agreements) {
            String otherCountry = agreement.getOtherCountry(playerCountry.getName());
            String status = agreement.isActive() ? "&a●" : "&7●";
            sender.sendMessage(ChatUtils.colorize(status + " &e" + otherCountry + 
                    " &7(Tax: " + ChatUtils.formatPercentage(agreement.getTaxRate()) + 
                    ", Expires: " + agreement.getDaysUntilExpiry() + " days)"));
        }
        
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    private void listAllRelations(CommandSender sender) {
        // Implementation for listing all diplomatic relations
        ChatUtils.sendInfo(sender, "Use '/diplomacy list <allies|enemies|trade>' for specific lists.");
    }
    
    private void handleGUI(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can use GUIs!");
            return;
        }
        
        if (!sender.hasPermission("countries.diplomacy.gui")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        plugin.getGUIListener().getDiplomacyGUI().openDiplomacyManagement(player);
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ChatUtils.colorize("&6&lDiplomacy Commands"));
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ChatUtils.colorize("&e/diplomacy ally <country> &7- Propose alliance"));
        sender.sendMessage(ChatUtils.colorize("&e/diplomacy war <country> [reason] &7- Declare war"));
        sender.sendMessage(ChatUtils.colorize("&e/diplomacy neutral <country> &7- Set neutral relations"));
        sender.sendMessage(ChatUtils.colorize("&e/diplomacy trade <country> [tax] [days] &7- Propose trade"));
        sender.sendMessage(ChatUtils.colorize("&e/diplomacy info [country] &7- View diplomatic info"));
        sender.sendMessage(ChatUtils.colorize("&e/diplomacy list <allies|enemies|trade> &7- List relations"));
        sender.sendMessage(ChatUtils.colorize("&e/diplomacy accept <type> <country> &7- Accept proposal"));
        sender.sendMessage(ChatUtils.colorize("&e/diplomacy reject <type> <country> &7- Reject proposal"));
        sender.sendMessage(ChatUtils.colorize("&e/diplomacy gui &7- Open diplomacy GUI"));
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommands
            String[] subCommands = {"ally", "war", "neutral", "trade", "info", "list", "accept", "reject", "help"};
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "ally", "war", "neutral", "trade", "info" -> {
                    // Country names (excluding player's own country)
                    if (sender instanceof Player player) {
                        Country playerCountry = plugin.getCountryManager().getPlayerCountry(player);
                        for (String countryName : plugin.getCountryManager().getCountryNames()) {
                            if (playerCountry == null || !countryName.equalsIgnoreCase(playerCountry.getName())) {
                                if (countryName.toLowerCase().startsWith(args[1].toLowerCase())) {
                                    completions.add(countryName);
                                }
                            }
                        }
                    }
                }
                case "list" -> {
                    String[] listTypes = {"allies", "enemies", "trade", "all"};
                    for (String listType : listTypes) {
                        if (listType.startsWith(args[1].toLowerCase())) {
                            completions.add(listType);
                        }
                    }
                }
                case "accept", "reject" -> {
                    String[] proposalTypes = {"alliance", "trade"};
                    for (String proposalType : proposalTypes) {
                        if (proposalType.startsWith(args[1].toLowerCase())) {
                            completions.add(proposalType);
                        }
                    }
                }
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            if ("accept".equals(subCommand) || "reject".equals(subCommand)) {
                // Country names for accept/reject
                if (sender instanceof Player player) {
                    Country playerCountry = plugin.getCountryManager().getPlayerCountry(player);
                    for (String countryName : plugin.getCountryManager().getCountryNames()) {
                        if (playerCountry == null || !countryName.equalsIgnoreCase(playerCountry.getName())) {
                            if (countryName.toLowerCase().startsWith(args[2].toLowerCase())) {
                                completions.add(countryName);
                            }
                        }
                    }
                }
            } else if ("trade".equals(subCommand)) {
                // Tax rate suggestions
                completions.add("0.05");
                completions.add("0.10");
                completions.add("0.15");
            }
        } else if (args.length == 4 && "trade".equals(args[0].toLowerCase())) {
            // Duration suggestions for trade
            completions.add("30");
            completions.add("60");
            completions.add("90");
        }
        
        return completions;
    }
}