package xyz.inv1s1bl3.countries.commands.country;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.country.GovernmentType;
import xyz.inv1s1bl3.countries.database.entities.Country;
import xyz.inv1s1bl3.countries.utils.MessageUtil;
import xyz.inv1s1bl3.countries.utils.PermissionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Main country command handler
 */
@RequiredArgsConstructor
public final class CountryCommand implements CommandExecutor, TabCompleter {
    
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
            case "create" -> this.handleCreate(sender, args);
            case "info" -> this.handleInfo(sender, args);
            case "dissolve" -> this.handleDissolve(sender, args);
            case "invite" -> this.handleInvite(sender, args);
            case "join" -> this.handleJoin(sender, args);
            case "leave" -> this.handleLeave(sender, args);
            case "kick" -> this.handleKick(sender, args);
            case "role" -> this.handleRole(sender, args);
            case "transfer" -> this.handleTransfer(sender, args);
            case "list" -> this.handleList(sender, args);
            case "help" -> this.sendHelpMessage(sender);
            default -> MessageUtil.sendMessage(sender, "general.invalid-command", 
                    Map.of("usage", "/country help"));
        }
        
        return true;
    }
    
    private void handleCreate(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "This command can only be used by players!");
            return;
        }
        
        if (!PermissionUtil.canCreateCountry(player)) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 2) {
            MessageUtil.sendMessage(sender, "country.create-usage");
            return;
        }
        
        final String countryName = args[1];
        final String governmentType = args.length > 2 ? args[2] : "democracy";
        
        try {
            final Country country = this.plugin.getCountryManager().createCountry(
                player.getUniqueId(), countryName, governmentType
            );
            
            MessageUtil.sendMessage(sender, "country.create-success", 
                Map.of("name", country.getName()));
            
        } catch (final IllegalArgumentException exception) {
            MessageUtil.sendMessage(sender, "country.create-failed", 
                Map.of("reason", exception.getMessage()));
        } catch (final Exception exception) {
            MessageUtil.sendMessage(sender, "general.database-error");
            this.plugin.getLogger().severe("Error creating country: " + exception.getMessage());
        }
    }
    
    private void handleInfo(final CommandSender sender, final String[] args) {
        String countryName = null;
        
        if (args.length > 1) {
            countryName = args[1];
        } else if (sender instanceof Player player) {
            final Optional<xyz.inv1s1bl3.countries.database.entities.Player> playerDataOpt = 
                this.plugin.getCountryManager().getPlayer(player.getUniqueId());
            
            if (playerDataOpt.isPresent() && playerDataOpt.get().hasCountry()) {
                final Optional<Country> countryOpt = this.plugin.getCountryManager()
                    .getCountry(playerDataOpt.get().getCountryId());
                if (countryOpt.isPresent()) {
                    countryName = countryOpt.get().getName();
                }
            }
        }
        
        if (countryName == null) {
            MessageUtil.sendError(sender, "Please specify a country name or join a country first!");
            return;
        }
        
        final Optional<Country> countryOpt = this.plugin.getCountryManager().getCountryByName(countryName);
        if (countryOpt.isEmpty()) {
            MessageUtil.sendError(sender, "Country '" + countryName + "' not found!");
            return;
        }
        
        final Country country = countryOpt.get();
        final List<xyz.inv1s1bl3.countries.database.entities.Player> members = 
            this.plugin.getCountryManager().getCountryMembers(country.getId());
        
        // Get leader name
        final Optional<xyz.inv1s1bl3.countries.database.entities.Player> leaderOpt = 
            members.stream().filter(p -> p.getUuid().equals(country.getLeaderUuid())).findFirst();
        final String leaderName = leaderOpt.map(xyz.inv1s1bl3.countries.database.entities.Player::getUsername)
            .orElse("Unknown");
        
        // Send country information
        MessageUtil.sendMessage(sender, "country.info-header", Map.of("name", country.getFormattedName()));
        MessageUtil.sendMessage(sender, "country.info-government", Map.of("government", country.getGovernmentType()));
        MessageUtil.sendMessage(sender, "country.info-leader", Map.of("leader", leaderName));
        MessageUtil.sendMessage(sender, "country.info-citizens", 
            Map.of("count", String.valueOf(members.size()), "max", "50"));
        MessageUtil.sendMessage(sender, "country.info-territory", Map.of("chunks", "0"));
        MessageUtil.sendMessage(sender, "country.info-treasury", 
            Map.of("balance", String.format("%.2f", country.getTreasuryBalance())));
        MessageUtil.sendMessage(sender, "country.info-founded", 
            Map.of("date", country.getCreatedAt().toLocalDate().toString()));
        
        if (!country.getDescription().isEmpty()) {
            MessageUtil.sendMessage(sender, "country.info-description", 
                Map.of("description", country.getDescription()));
        }
    }
    
    private void handleDissolve(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "This command can only be used by players!");
            return;
        }
        
        if (!PermissionUtil.canDissolveCountry(player)) {
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
        final Optional<Country> countryOpt = this.plugin.getCountryManager().getCountry(playerData.getCountryId());
        
        if (countryOpt.isEmpty()) {
            MessageUtil.sendMessage(sender, "general.database-error");
            return;
        }
        
        final Country country = countryOpt.get();
        
        if (!country.isLeader(player.getUniqueId())) {
            MessageUtil.sendMessage(sender, "country.dissolve-not-leader");
            return;
        }
        
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            MessageUtil.sendMessage(sender, "country.dissolve-confirm", Map.of("name", country.getName()));
            return;
        }
        
        try {
            this.plugin.getCountryManager().dissolveCountry(country.getId(), player.getUniqueId());
            MessageUtil.sendMessage(sender, "country.dissolve-success", Map.of("name", country.getName()));
            
        } catch (final Exception exception) {
            MessageUtil.sendMessage(sender, "country.dissolve-failed", 
                Map.of("reason", exception.getMessage()));
        }
    }
    
    private void handleInvite(final CommandSender sender, final String[] args) {
        // TODO: Implement invitation system
        MessageUtil.sendError(sender, "Invitation system not yet implemented!");
    }
    
    private void handleJoin(final CommandSender sender, final String[] args) {
        // TODO: Implement join system
        MessageUtil.sendError(sender, "Join system not yet implemented!");
    }
    
    private void handleLeave(final CommandSender sender, final String[] args) {
        // TODO: Implement leave system
        MessageUtil.sendError(sender, "Leave system not yet implemented!");
    }
    
    private void handleKick(final CommandSender sender, final String[] args) {
        // TODO: Implement kick system
        MessageUtil.sendError(sender, "Kick system not yet implemented!");
    }
    
    private void handleRole(final CommandSender sender, final String[] args) {
        // TODO: Implement role management
        MessageUtil.sendError(sender, "Role management not yet implemented!");
    }
    
    private void handleTransfer(final CommandSender sender, final String[] args) {
        // TODO: Implement leadership transfer
        MessageUtil.sendError(sender, "Leadership transfer not yet implemented!");
    }
    
    private void handleList(final CommandSender sender, final String[] args) {
        final List<Country> countries = this.plugin.getCountryManager().getAllActiveCountries();
        
        if (countries.isEmpty()) {
            MessageUtil.sendInfo(sender, "No countries exist yet!");
            return;
        }
        
        MessageUtil.sendInfo(sender, "Active Countries (" + countries.size() + "):");
        for (final Country country : countries) {
            final int memberCount = this.plugin.getCountryManager().getCountryMembers(country.getId()).size();
            MessageUtil.sendInfo(sender, "- " + country.getFormattedName() + 
                " (" + country.getGovernmentType() + ", " + memberCount + " citizens)");
        }
    }
    
    private void sendHelpMessage(final CommandSender sender) {
        MessageUtil.sendInfo(sender, "Country Commands:");
        MessageUtil.sendInfo(sender, "/country create <name> [government] - Create a new country");
        MessageUtil.sendInfo(sender, "/country info [name] - View country information");
        MessageUtil.sendInfo(sender, "/country dissolve confirm - Dissolve your country");
        MessageUtil.sendInfo(sender, "/country list - List all countries");
        MessageUtil.sendInfo(sender, "/country invite <player> - Invite a player to your country");
        MessageUtil.sendInfo(sender, "/country join <country> - Join a country");
        MessageUtil.sendInfo(sender, "/country leave - Leave your country");
        MessageUtil.sendInfo(sender, "/country kick <player> - Kick a player from your country");
        MessageUtil.sendInfo(sender, "/country role <player> <role> - Set a player's role");
        MessageUtil.sendInfo(sender, "/country transfer <player> - Transfer leadership");
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final Command command, 
                                               @NotNull final String alias, @NotNull final String[] args) {
        
        if (args.length == 1) {
            return Arrays.asList("create", "info", "dissolve", "invite", "join", "leave", 
                               "kick", "role", "transfer", "list", "help")
                .stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "create" -> {
                    return new ArrayList<>(); // No tab completion for country names
                }
                case "info" -> {
                    return this.plugin.getCountryManager().getAllActiveCountries()
                        .stream()
                        .map(Country::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                }
            }
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            return Arrays.stream(GovernmentType.values())
                .map(GovernmentType::getKey)
                .filter(key -> key.toLowerCase().startsWith(args[2].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}