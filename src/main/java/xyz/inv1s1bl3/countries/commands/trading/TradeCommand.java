package xyz.inv1s1bl3.countries.commands.trading;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.trading.TradeSession;
import xyz.inv1s1bl3.countries.utils.MessageUtil;
import xyz.inv1s1bl3.countries.utils.PermissionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Trade command handler
 */
@RequiredArgsConstructor
public final class TradeCommand implements CommandExecutor, TabCompleter {
    
    private final CountriesPlugin plugin;
    
    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, 
                           @NotNull final String label, @NotNull final String[] args) {
        
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "This command can only be used by players!");
            return true;
        }
        
        if (args.length == 0) {
            this.sendHelpMessage(sender);
            return true;
        }
        
        final String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "request", "invite" -> this.handleRequest(player, args);
            case "accept" -> this.handleAccept(player, args);
            case "decline", "deny" -> this.handleDecline(player, args);
            case "cancel" -> this.handleCancel(player);
            case "ready" -> this.handleReady(player);
            case "confirm" -> this.handleConfirm(player);
            case "status", "info" -> this.handleStatus(player);
            case "help" -> this.sendHelpMessage(sender);
            default -> MessageUtil.sendMessage(sender, "general.invalid-command", 
                    Map.of("usage", "/trade help"));
        }
        
        return true;
    }
    
    private void handleRequest(final Player player, final String[] args) {
        if (!PermissionUtil.canInitiateTrade(player)) {
            MessageUtil.sendMessage(player, "general.no-permission");
            return;
        }
        
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /trade request <player>");
            return;
        }
        
        final String targetName = args[1];
        final Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            MessageUtil.sendMessage(player, "general.player-not-found", Map.of("player", targetName));
            return;
        }
        
        if (target.equals(player)) {
            MessageUtil.sendError(player, "You cannot trade with yourself!");
            return;
        }
        
        try {
            final String sessionId = this.plugin.getTradingManager().createTradeSession(
                player.getUniqueId(), target.getUniqueId()
            );
            
            MessageUtil.sendMessage(player, "trading.trade-request-sent", 
                Map.of("player", target.getName()));
            
            MessageUtil.sendMessage(target, "trading.trade-request-received", 
                Map.of("player", player.getName()));
            
            // Send clickable accept/decline buttons
            MessageUtil.sendConfirmation(target, 
                "Click to respond to the trade request:",
                "/trade accept " + player.getName(),
                "/trade decline " + player.getName()
            );
            
        } catch (final IllegalArgumentException exception) {
            MessageUtil.sendError(player, exception.getMessage());
        }
    }
    
    private void handleAccept(final Player player, final String[] args) {
        final Optional<TradeSession> sessionOpt = this.plugin.getTradingManager()
            .getPlayerTradeSession(player.getUniqueId());
        
        if (sessionOpt.isEmpty()) {
            MessageUtil.sendError(player, "You don't have any pending trade requests!");
            return;
        }
        
        final TradeSession session = sessionOpt.get();
        
        // Check if this player is trader2 (the one who received the request)
        if (!session.isTrader2(player.getUniqueId())) {
            MessageUtil.sendError(player, "You cannot accept your own trade request!");
            return;
        }
        
        MessageUtil.sendMessage(player, "trading.trade-accepted");
        
        final Player otherPlayer = Bukkit.getPlayer(session.getTrader1Uuid());
        if (otherPlayer != null) {
            MessageUtil.sendMessage(otherPlayer, "trading.trade-accepted");
            MessageUtil.sendMessage(otherPlayer, "trading.trade-started", 
                Map.of("player", player.getName()));
        }
        
        MessageUtil.sendMessage(player, "trading.trade-started", 
            Map.of("player", otherPlayer != null ? otherPlayer.getName() : "Unknown"));
        
        // TODO: Open trade GUI when GUI system is implemented
        MessageUtil.sendInfo(player, "Trade GUI will open here when implemented!");
    }
    
    private void handleDecline(final Player player, final String[] args) {
        final Optional<TradeSession> sessionOpt = this.plugin.getTradingManager()
            .getPlayerTradeSession(player.getUniqueId());
        
        if (sessionOpt.isEmpty()) {
            MessageUtil.sendError(player, "You don't have any pending trade requests!");
            return;
        }
        
        final TradeSession session = sessionOpt.get();
        
        MessageUtil.sendMessage(player, "trading.trade-declined");
        
        final Player otherPlayer = Bukkit.getPlayer(session.getOtherTrader(player.getUniqueId()));
        if (otherPlayer != null) {
            MessageUtil.sendMessage(otherPlayer, "trading.trade-declined");
        }
        
        this.plugin.getTradingManager().cancelTrade(player.getUniqueId());
    }
    
    private void handleCancel(final Player player) {
        final Optional<TradeSession> sessionOpt = this.plugin.getTradingManager()
            .getPlayerTradeSession(player.getUniqueId());
        
        if (sessionOpt.isEmpty()) {
            MessageUtil.sendError(player, "You are not in a trade session!");
            return;
        }
        
        this.plugin.getTradingManager().cancelTrade(player.getUniqueId());
        MessageUtil.sendMessage(player, "trading.trade-cancelled");
    }
    
    private void handleReady(final Player player) {
        final Optional<TradeSession> sessionOpt = this.plugin.getTradingManager()
            .getPlayerTradeSession(player.getUniqueId());
        
        if (sessionOpt.isEmpty()) {
            MessageUtil.sendError(player, "You are not in a trade session!");
            return;
        }
        
        final TradeSession session = sessionOpt.get();
        final boolean currentReady = session.isPlayerReady(player.getUniqueId());
        
        try {
            this.plugin.getTradingManager().setPlayerReady(player.getUniqueId(), !currentReady);
            
            if (!currentReady) {
                MessageUtil.sendMessage(player, "trading.trade-ready");
                
                final Player otherPlayer = Bukkit.getPlayer(session.getOtherTrader(player.getUniqueId()));
                if (otherPlayer != null) {
                    if (session.areBothReady()) {
                        MessageUtil.sendInfo(otherPlayer, "Both players are ready! Type /trade confirm to complete the trade.");
                        MessageUtil.sendInfo(player, "Both players are ready! Type /trade confirm to complete the trade.");
                    } else {
                        MessageUtil.sendMessage(otherPlayer, "trading.trade-not-ready");
                    }
                }
            } else {
                MessageUtil.sendInfo(player, "You are no longer ready to trade.");
            }
            
        } catch (final IllegalArgumentException exception) {
            MessageUtil.sendError(player, exception.getMessage());
        }
    }
    
    private void handleConfirm(final Player player) {
        final Optional<TradeSession> sessionOpt = this.plugin.getTradingManager()
            .getPlayerTradeSession(player.getUniqueId());
        
        if (sessionOpt.isEmpty()) {
            MessageUtil.sendError(player, "You are not in a trade session!");
            return;
        }
        
        try {
            this.plugin.getTradingManager().confirmTrade(player.getUniqueId());
            MessageUtil.sendInfo(player, "Trade confirmed! Waiting for other player...");
            
        } catch (final IllegalArgumentException exception) {
            MessageUtil.sendError(player, exception.getMessage());
        }
    }
    
    private void handleStatus(final Player player) {
        final Optional<TradeSession> sessionOpt = this.plugin.getTradingManager()
            .getPlayerTradeSession(player.getUniqueId());
        
        if (sessionOpt.isEmpty()) {
            MessageUtil.sendInfo(player, "You are not in a trade session.");
            return;
        }
        
        final TradeSession session = sessionOpt.get();
        final Player otherPlayer = Bukkit.getPlayer(session.getOtherTrader(player.getUniqueId()));
        
        MessageUtil.sendInfo(player, "=== Trade Status ===");
        MessageUtil.sendInfo(player, "Trading with: " + (otherPlayer != null ? otherPlayer.getName() : "Unknown"));
        MessageUtil.sendInfo(player, "Your status: " + (session.isPlayerReady(player.getUniqueId()) ? "Ready" : "Not Ready"));
        MessageUtil.sendInfo(player, "Their status: " + (session.isPlayerReady(session.getOtherTrader(player.getUniqueId())) ? "Ready" : "Not Ready"));
        MessageUtil.sendInfo(player, "Your money offer: $" + String.format("%.2f", session.getPlayerMoney(player.getUniqueId())));
        MessageUtil.sendInfo(player, "Your items: " + session.getPlayerItems(player.getUniqueId()).size() + " items");
        MessageUtil.sendInfo(player, "Time remaining: " + session.getMinutesUntilExpiration() + " minutes");
        
        if (session.areBothReady()) {
            MessageUtil.sendInfo(player, "Both players ready! Use /trade confirm to complete.");
        }
    }
    
    private void sendHelpMessage(final CommandSender sender) {
        MessageUtil.sendInfo(sender, "Trade Commands:");
        MessageUtil.sendInfo(sender, "/trade request <player> - Send a trade request");
        MessageUtil.sendInfo(sender, "/trade accept - Accept a trade request");
        MessageUtil.sendInfo(sender, "/trade decline - Decline a trade request");
        MessageUtil.sendInfo(sender, "/trade cancel - Cancel current trade");
        MessageUtil.sendInfo(sender, "/trade ready - Toggle ready status");
        MessageUtil.sendInfo(sender, "/trade confirm - Confirm and complete trade");
        MessageUtil.sendInfo(sender, "/trade status - View trade status");
        MessageUtil.sendInfo(sender, "");
        MessageUtil.sendInfo(sender, "Note: GUI-based trading will be available when the GUI system is implemented.");
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final Command command, 
                                               @NotNull final String alias, @NotNull final String[] args) {
        
        if (args.length == 1) {
            return Arrays.asList("request", "accept", "decline", "cancel", "ready", "confirm", "status", "help")
                .stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "request", "invite" -> {
                    return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                }
                case "accept", "decline" -> {
                    // Get players who have sent trade requests to this player
                    if (sender instanceof Player player) {
                        final Optional<TradeSession> sessionOpt = this.plugin.getTradingManager()
                            .getPlayerTradeSession(player.getUniqueId());
                        
                        if (sessionOpt.isPresent()) {
                            final TradeSession session = sessionOpt.get();
                            final Player otherPlayer = Bukkit.getPlayer(session.getOtherTrader(player.getUniqueId()));
                            if (otherPlayer != null) {
                                return List.of(otherPlayer.getName());
                            }
                        }
                    }
                }
            }
        }
        
        return new ArrayList<>();
    }
}