package xyz.inv1s1bl3.countries.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all economy-related commands.
 * Will be fully implemented in Phase 4 - Economy System.
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
        
        // TODO: Implement economy commands
        ChatUtils.sendInfo(player, "&7Economy system coming in Phase 4!");
        
        return true;
    }
    
    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull final CommandSender sender, 
                                    @NotNull final Command command, 
                                    @NotNull final String alias, 
                                    @NotNull final String[] args) {
        return new ArrayList<>();
    }
}