package xyz.inv1s1bl3.countries.economy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import xyz.inv1s1bl3.countries.CountriesPlugin;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles Vault economy integration
 */
@Getter
@RequiredArgsConstructor
public final class VaultIntegration {
    
    private final CountriesPlugin plugin;
    private final Economy vaultEconomy;
    
    /**
     * Check if Vault economy is available
     * @return true if Vault economy is available
     */
    public boolean isAvailable() {
        return this.vaultEconomy != null;
    }
    
    /**
     * Get player's balance
     * @param player Player to check
     * @return Player's balance
     */
    public double getBalance(final OfflinePlayer player) {
        if (!this.isAvailable()) {
            return 0.0;
        }
        
        return this.vaultEconomy.getBalance(player);
    }
    
    /**
     * Get player's balance by UUID
     * @param playerUuid Player UUID
     * @return Player's balance
     */
    public double getBalance(final UUID playerUuid) {
        if (!this.isAvailable()) {
            return 0.0;
        }
        
        final OfflinePlayer player = this.plugin.getServer().getOfflinePlayer(playerUuid);
        return this.vaultEconomy.getBalance(player);
    }
    
    /**
     * Check if player has enough money
     * @param player Player to check
     * @param amount Amount to check
     * @return true if player has enough money
     */
    public boolean hasBalance(final OfflinePlayer player, final double amount) {
        if (!this.isAvailable()) {
            return false;
        }
        
        return this.vaultEconomy.has(player, amount);
    }
    
    /**
     * Withdraw money from player
     * @param player Player to withdraw from
     * @param amount Amount to withdraw
     * @return true if successful
     */
    public boolean withdrawPlayer(final OfflinePlayer player, final double amount) {
        if (!this.isAvailable()) {
            return false;
        }
        
        final EconomyResponse response = this.vaultEconomy.withdrawPlayer(player, amount);
        
        if (!response.transactionSuccess()) {
            this.plugin.getLogger().warning("Failed to withdraw $" + amount + " from " + player.getName() + ": " + response.errorMessage);
            return false;
        }
        
        return true;
    }
    
    /**
     * Deposit money to player
     * @param player Player to deposit to
     * @param amount Amount to deposit
     * @return true if successful
     */
    public boolean depositPlayer(final OfflinePlayer player, final double amount) {
        if (!this.isAvailable()) {
            return false;
        }
        
        final EconomyResponse response = this.vaultEconomy.depositPlayer(player, amount);
        
        if (!response.transactionSuccess()) {
            this.plugin.getLogger().warning("Failed to deposit $" + amount + " to " + player.getName() + ": " + response.errorMessage);
            return false;
        }
        
        return true;
    }
    
    /**
     * Transfer money between players
     * @param from Player to transfer from
     * @param to Player to transfer to
     * @param amount Amount to transfer
     * @return true if successful
     */
    public boolean transferMoney(final OfflinePlayer from, final OfflinePlayer to, final double amount) {
        if (!this.isAvailable()) {
            return false;
        }
        
        // Check if sender has enough money
        if (!this.hasBalance(from, amount)) {
            return false;
        }
        
        // Withdraw from sender
        if (!this.withdrawPlayer(from, amount)) {
            return false;
        }
        
        // Deposit to receiver
        if (!this.depositPlayer(to, amount)) {
            // Refund sender if deposit fails
            this.depositPlayer(from, amount);
            return false;
        }
        
        return true;
    }
    
    /**
     * Format money amount
     * @param amount Amount to format
     * @return Formatted money string
     */
    public String formatMoney(final double amount) {
        if (!this.isAvailable()) {
            return String.format("$%.2f", amount);
        }
        
        return this.vaultEconomy.format(amount);
    }
    
    /**
     * Get currency name (singular)
     * @return Currency name
     */
    public String getCurrencyNameSingular() {
        if (!this.isAvailable()) {
            return "dollar";
        }
        
        return this.vaultEconomy.currencyNameSingular();
    }
    
    /**
     * Get currency name (plural)
     * @return Currency name
     */
    public String getCurrencyNamePlural() {
        if (!this.isAvailable()) {
            return "dollars";
        }
        
        return this.vaultEconomy.currencyNamePlural();
    }
    
    /**
     * Create a bank account for a country
     * @param countryName Country name
     * @param ownerUuid Owner UUID
     * @return true if successful
     */
    public boolean createBankAccount(final String countryName, final UUID ownerUuid) {
        if (!this.isAvailable()) {
            return false;
        }
        
        final OfflinePlayer owner = this.plugin.getServer().getOfflinePlayer(ownerUuid);
        final EconomyResponse response = this.vaultEconomy.createBank(countryName, owner);
        
        if (!response.transactionSuccess()) {
            this.plugin.getLogger().warning("Failed to create bank account for " + countryName + ": " + response.errorMessage);
            return false;
        }
        
        return true;
    }
    
    /**
     * Delete a bank account
     * @param countryName Country name
     * @return true if successful
     */
    public boolean deleteBankAccount(final String countryName) {
        if (!this.isAvailable()) {
            return false;
        }
        
        final EconomyResponse response = this.vaultEconomy.deleteBank(countryName);
        
        if (!response.transactionSuccess()) {
            this.plugin.getLogger().warning("Failed to delete bank account for " + countryName + ": " + response.errorMessage);
            return false;
        }
        
        return true;
    }
    
    /**
     * Get bank balance
     * @param countryName Country name
     * @return Bank balance
     */
    public double getBankBalance(final String countryName) {
        if (!this.isAvailable()) {
            return 0.0;
        }
        
        final EconomyResponse response = this.vaultEconomy.bankBalance(countryName);
        
        if (!response.transactionSuccess()) {
            return 0.0;
        }
        
        return response.balance;
    }
    
    /**
     * Deposit to bank
     * @param countryName Country name
     * @param amount Amount to deposit
     * @return true if successful
     */
    public boolean depositBank(final String countryName, final double amount) {
        if (!this.isAvailable()) {
            return false;
        }
        
        final EconomyResponse response = this.vaultEconomy.bankDeposit(countryName, amount);
        
        if (!response.transactionSuccess()) {
            this.plugin.getLogger().warning("Failed to deposit $" + amount + " to bank " + countryName + ": " + response.errorMessage);
            return false;
        }
        
        return true;
    }
    
    /**
     * Withdraw from bank
     * @param countryName Country name
     * @param amount Amount to withdraw
     * @return true if successful
     */
    public boolean withdrawBank(final String countryName, final double amount) {
        if (!this.isAvailable()) {
            return false;
        }
        
        final EconomyResponse response = this.vaultEconomy.bankWithdraw(countryName, amount);
        
        if (!response.transactionSuccess()) {
            this.plugin.getLogger().warning("Failed to withdraw $" + amount + " from bank " + countryName + ": " + response.errorMessage);
            return false;
        }
        
        return true;
    }
}