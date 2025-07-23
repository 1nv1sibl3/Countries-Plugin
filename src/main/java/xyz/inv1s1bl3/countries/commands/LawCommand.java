package xyz.inv1s1bl3.countries.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.country.Country;
import xyz.inv1s1bl3.countries.core.law.Crime;
import xyz.inv1s1bl3.countries.core.law.CrimeType;
import xyz.inv1s1bl3.countries.core.law.Law;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.*;

/**
 * Handles all law and order related commands.
 */
public class LawCommand implements CommandExecutor, TabCompleter {
    
    private final CountriesPlugin plugin;
    
    public LawCommand(CountriesPlugin plugin) {
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
            case "create" -> handleCreateLaw(sender, args);
            case "list" -> handleListLaws(sender, args);
            case "report" -> handleReportCrime(sender, args);
            case "arrest" -> handleArrest(sender, args);
            case "release" -> handleRelease(sender, args);
            case "fine" -> handleFine(sender, args);
            case "bounty" -> handleBounty(sender, args);
            case "crimes" -> handleCrimes(sender, args);
            case "jail" -> handleJailStatus(sender, args);
            case "help" -> sendHelp(sender);
            default -> {
                ChatUtils.sendError(sender, "Unknown subcommand. Use /law help for available commands.");
                sendHelp(sender);
            }
        }
        
        return true;
    }
    
    private void handleCreateLaw(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can create laws!");
            return;
        }
        
        if (args.length < 5) {
            ChatUtils.sendError(sender, "Usage: /law create <title> <fine> <jail_minutes> <description>");
            return;
        }
        
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-member");
            return;
        }
        
        String title = args[1];
        double fine;
        int jailTime;
        
        try {
            fine = Double.parseDouble(args[2]);
            jailTime = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            ChatUtils.sendError(sender, "Invalid fine amount or jail time!");
            return;
        }
        
        String description = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
        
        if (plugin.getLawSystem().createLaw(country.getName(), title, description, 
                fine, jailTime, player.getUniqueId())) {
            ChatUtils.sendSuccess(sender, "Law '" + title + "' created successfully!");
        } else {
            ChatUtils.sendError(sender, "Failed to create law. You may not have permission.");
        }
    }
    
    private void handleListLaws(CommandSender sender, String[] args) {
        String countryName;
        
        if (args.length >= 2) {
            countryName = args[1];
        } else if (sender instanceof Player player) {
            Country country = plugin.getCountryManager().getPlayerCountry(player);
            if (country == null) {
                ChatUtils.sendPrefixedConfigMessage(sender, "country.not-member");
                return;
            }
            countryName = country.getName();
        } else {
            ChatUtils.sendError(sender, "Console must specify a country name!");
            return;
        }
        
        List<Law> laws = plugin.getLawSystem().getCountryLaws(countryName);
        
        if (laws.isEmpty()) {
            ChatUtils.sendInfo(sender, "No laws found for " + countryName + "!");
            return;
        }
        
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ChatUtils.colorize("&6&lLaws of " + countryName + " &7(" + laws.size() + " total)"));
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        for (Law law : laws) {
            String status = law.isActive() ? "&a●" : "&7●";
            sender.sendMessage(ChatUtils.colorize(status + " &e" + law.getTitle() + 
                    " &7(Fine: " + ChatUtils.formatCurrency(law.getFineAmount()) + 
                    ", Jail: " + law.getJailTime() + "min)"));
        }
        
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    private void handleReportCrime(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can report crimes!");
            return;
        }
        
        if (args.length < 4) {
            ChatUtils.sendError(sender, "Usage: /law report <player> <crime_type> <description>");
            return;
        }
        
        Player criminal = Bukkit.getPlayer(args[1]);
        if (criminal == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.player-not-found", args[1]);
            return;
        }
        
        CrimeType crimeType = CrimeType.fromString(args[2]);
        String description = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-member");
            return;
        }
        
        if (plugin.getLawSystem().reportCrime(criminal.getUniqueId(), criminal.getName(), 
                player.getUniqueId(), player.getName(), crimeType, country.getName(), 
                description, "Player report")) {
            ChatUtils.sendSuccess(sender, "Crime reported successfully!");
            ChatUtils.sendInfo(sender, "Crime: " + crimeType.getDisplayName() + " by " + criminal.getName());
        } else {
            ChatUtils.sendError(sender, "Failed to report crime.");
        }
    }
    
    private void handleArrest(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can make arrests!");
            return;
        }
        
        if (args.length < 3) {
            ChatUtils.sendError(sender, "Usage: /law arrest <player> <crime_id>");
            return;
        }
        
        Player criminal = Bukkit.getPlayer(args[1]);
        if (criminal == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.player-not-found", args[1]);
            return;
        }
        
        int crimeId;
        try {
            crimeId = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            ChatUtils.sendError(sender, "Invalid crime ID!");
            return;
        }
        
        if (plugin.getLawSystem().arrestPlayer(criminal.getUniqueId(), player.getUniqueId(), crimeId)) {
            ChatUtils.sendSuccess(sender, "Player " + criminal.getName() + " has been arrested!");
        } else {
            ChatUtils.sendError(sender, "Failed to arrest player. Check permissions and crime ID.");
        }
    }
    
    private void handleRelease(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can release prisoners!");
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(sender, "Usage: /law release <player>");
            return;
        }
        
        Player prisoner = Bukkit.getPlayer(args[1]);
        if (prisoner == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.player-not-found", args[1]);
            return;
        }
        
        if (plugin.getLawSystem().releasePlayer(prisoner.getUniqueId())) {
            ChatUtils.sendSuccess(sender, "Player " + prisoner.getName() + " has been released!");
        } else {
            ChatUtils.sendError(sender, "Player is not in jail or cannot be released.");
        }
    }
    
    private void handleFine(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can issue fines!");
            return;
        }
        
        if (args.length < 3) {
            ChatUtils.sendError(sender, "Usage: /law fine <player> <amount>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.player-not-found", args[1]);
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            ChatUtils.sendError(sender, "Invalid fine amount!");
            return;
        }
        
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-member");
            return;
        }
        
        if (plugin.getLawSystem().issueFine(target.getUniqueId(), amount, country.getName())) {
            ChatUtils.sendSuccess(sender, "Fine of " + ChatUtils.formatCurrency(amount) + 
                    " issued to " + target.getName() + "!");
        } else {
            ChatUtils.sendError(sender, "Failed to issue fine.");
        }
    }
    
    private void handleBounty(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can manage bounties!");
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(sender, "Usage: /law bounty <set|claim|check> [player] [amount]");
            return;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "set" -> {
                if (args.length < 4) {
                    ChatUtils.sendError(sender, "Usage: /law bounty set <player> <amount>");
                    return;
                }
                
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    ChatUtils.sendPrefixedConfigMessage(sender, "general.player-not-found", args[2]);
                    return;
                }
                
                double amount;
                try {
                    amount = Double.parseDouble(args[3]);
                } catch (NumberFormatException e) {
                    ChatUtils.sendError(sender, "Invalid bounty amount!");
                    return;
                }
                
                if (plugin.getLawSystem().setBounty(target.getUniqueId(), amount, player.getUniqueId())) {
                    ChatUtils.sendSuccess(sender, "Bounty of " + ChatUtils.formatCurrency(amount) + 
                            " set on " + target.getName() + "!");
                } else {
                    ChatUtils.sendError(sender, "Failed to set bounty. Check your balance.");
                }
            }
            case "claim" -> {
                if (args.length < 3) {
                    ChatUtils.sendError(sender, "Usage: /law bounty claim <player>");
                    return;
                }
                
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    ChatUtils.sendPrefixedConfigMessage(sender, "general.player-not-found", args[2]);
                    return;
                }
                
                if (plugin.getLawSystem().claimBounty(target.getUniqueId(), player.getUniqueId())) {
                    ChatUtils.sendSuccess(sender, "Bounty claimed on " + target.getName() + "!");
                } else {
                    ChatUtils.sendError(sender, "No bounty available or failed to claim.");
                }
            }
            case "check" -> {
                if (args.length < 3) {
                    ChatUtils.sendError(sender, "Usage: /law bounty check <player>");
                    return;
                }
                
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    ChatUtils.sendPrefixedConfigMessage(sender, "general.player-not-found", args[2]);
                    return;
                }
                
                double bounty = plugin.getLawSystem().getBounty(target.getUniqueId());
                if (bounty > 0) {
                    ChatUtils.sendInfo(sender, "Bounty on " + target.getName() + ": " + 
                            ChatUtils.formatCurrency(bounty));
                } else {
                    ChatUtils.sendInfo(sender, "No bounty on " + target.getName());
                }
            }
            default -> ChatUtils.sendError(sender, "Invalid bounty action! Use set, claim, or check.");
        }
    }
    
    private void handleCrimes(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can view crimes!");
            return;
        }
        
        UUID targetUUID = player.getUniqueId();
        String targetName = player.getName();
        
        if (args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null) {
                targetUUID = target.getUniqueId();
                targetName = target.getName();
            }
        }
        
        List<Crime> crimes = plugin.getLawSystem().getPlayerCrimes(targetUUID);
        
        if (crimes.isEmpty()) {
            ChatUtils.sendInfo(sender, "No crimes found for " + targetName + "!");
            return;
        }
        
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ChatUtils.colorize("&6&lCrimes for " + targetName + " &7(" + crimes.size() + " total)"));
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        for (Crime crime : crimes) {
            sender.sendMessage(ChatUtils.colorize("&7ID: " + crime.getCrimeId() + " | " + 
                    crime.getCrimeType().getSeverityColor() + crime.getCrimeType().getDisplayName() + 
                    " | " + crime.getStatusDisplay()));
        }
        
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    private void handleJailStatus(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can check jail status!");
            return;
        }
        
        UUID targetUUID = player.getUniqueId();
        String targetName = player.getName();
        
        if (args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null) {
                targetUUID = target.getUniqueId();
                targetName = target.getName();
            }
        }
        
        if (plugin.getLawSystem().isPlayerInJail(targetUUID)) {
            long remainingTime = plugin.getLawSystem().getRemainingJailTime(targetUUID);
            ChatUtils.sendInfo(sender, targetName + " is in jail for " + remainingTime + " more minutes.");
        } else {
            ChatUtils.sendInfo(sender, targetName + " is not in jail.");
        }
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ChatUtils.colorize("&6&lLaw & Order Commands"));
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ChatUtils.colorize("&e/law create <title> <fine> <jail> <desc> &7- Create law"));
        sender.sendMessage(ChatUtils.colorize("&e/law list [country] &7- List country laws"));
        sender.sendMessage(ChatUtils.colorize("&e/law report <player> <crime> <desc> &7- Report crime"));
        sender.sendMessage(ChatUtils.colorize("&e/law arrest <player> <crime_id> &7- Arrest player"));
        sender.sendMessage(ChatUtils.colorize("&e/law release <player> &7- Release from jail"));
        sender.sendMessage(ChatUtils.colorize("&e/law fine <player> <amount> &7- Issue fine"));
        sender.sendMessage(ChatUtils.colorize("&e/law bounty <set|claim|check> <player> [amount] &7- Manage bounties"));
        sender.sendMessage(ChatUtils.colorize("&e/law crimes [player] &7- View crime history"));
        sender.sendMessage(ChatUtils.colorize("&e/law jail [player] &7- Check jail status"));
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String[] subCommands = {"create", "list", "report", "arrest", "release", "fine", "bounty", "crimes", "jail", "help"};
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "list" -> {
                    // Country names
                    for (String countryName : plugin.getCountryManager().getCountryNames()) {
                        if (countryName.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(countryName);
                        }
                    }
                }
                case "report", "arrest", "release", "fine", "crimes", "jail" -> {
                    // Online players
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(player.getName());
                        }
                    }
                }
                case "bounty" -> {
                    String[] bountyActions = {"set", "claim", "check"};
                    for (String action : bountyActions) {
                        if (action.startsWith(args[1].toLowerCase())) {
                            completions.add(action);
                        }
                    }
                }
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            if ("report".equals(subCommand)) {
                // Crime types
                for (CrimeType crimeType : CrimeType.values()) {
                    if (crimeType.getDisplayName().toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(crimeType.getDisplayName());
                    }
                }
            } else if ("bounty".equals(subCommand)) {
                // Online players for bounty actions
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            }
        }
        
        return completions;
    }
}