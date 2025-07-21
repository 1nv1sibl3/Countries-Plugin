package xyz.inv1s1bl3.countries.commands;

import lombok.RequiredArgsConstructor;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.commands.country.CountryCommand;
import xyz.inv1s1bl3.countries.commands.territory.TerritoryCommand;
import xyz.inv1s1bl3.countries.commands.economy.EconomyCommand;
import xyz.inv1s1bl3.countries.commands.admin.AdminCommand;
import xyz.inv1s1bl3.countries.commands.trading.TradeCommand;

/**
 * Manages all plugin commands
 */
@RequiredArgsConstructor
public final class CommandManager {
    
    private final CountriesPlugin plugin;
    
    /**
     * Register all commands
     */
    public void registerCommands() {
        this.plugin.getLogger().info("Registering commands...");
        
        // Register command executors
        this.plugin.getCommand("country").setExecutor(new CountryCommand(this.plugin));
        this.plugin.getCommand("territory").setExecutor(new TerritoryCommand(this.plugin));
        this.plugin.getCommand("economy").setExecutor(new EconomyCommand(this.plugin));
        this.plugin.getCommand("countries-admin").setExecutor(new AdminCommand(this.plugin));
        this.plugin.getCommand("trade").setExecutor(new TradeCommand(this.plugin));
        // this.plugin.getCommand("diplomacy").setExecutor(new DiplomacyCommand(this.plugin));
        // this.plugin.getCommand("legal").setExecutor(new LegalCommand(this.plugin));
        
        this.plugin.getLogger().info("Commands registered successfully!");
    }
}