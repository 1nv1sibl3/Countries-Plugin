package xyz.inv1s1bl3.countries.core.diplomacy;

import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a trade agreement between two countries.
 */
public class TradeAgreement {
    
    private final String country1;
    private final String country2;
    private final long createdDate;
    private long expiryDate;
    private boolean active;
    private double taxRate;
    private double totalVolume;
    
    // Trade terms
    private final Map<String, Double> exportTariffs; // Item type -> tariff rate
    private final Map<String, Double> importTariffs;
    private final Map<String, Integer> quotas; // Item type -> max quantity
    
    // Statistics
    private int totalTransactions;
    private long lastTradeDate;
    
    public TradeAgreement(String country1, String country2, double taxRate, long durationDays) {
        this.country1 = country1;
        this.country2 = country2;
        this.createdDate = System.currentTimeMillis();
        this.expiryDate = createdDate + (durationDays * 86400000L); // Convert days to milliseconds
        this.active = true;
        this.taxRate = Math.max(0, Math.min(1, taxRate));
        this.totalVolume = 0.0;
        
        this.exportTariffs = new HashMap<>();
        this.importTariffs = new HashMap<>();
        this.quotas = new HashMap<>();
        
        this.totalTransactions = 0;
        this.lastTradeDate = 0L;
    }
    
    // Getters
    public String getCountry1() {
        return country1;
    }
    
    public String getCountry2() {
        return country2;
    }
    
    public long getCreatedDate() {
        return createdDate;
    }
    
    public long getExpiryDate() {
        return expiryDate;
    }
    
    public boolean isActive() {
        return active && System.currentTimeMillis() < expiryDate;
    }
    
    public double getTaxRate() {
        return taxRate;
    }
    
    public double getTotalVolume() {
        return totalVolume;
    }
    
    public Map<String, Double> getExportTariffs() {
        return new HashMap<>(exportTariffs);
    }
    
    public Map<String, Double> getImportTariffs() {
        return new HashMap<>(importTariffs);
    }
    
    public Map<String, Integer> getQuotas() {
        return new HashMap<>(quotas);
    }
    
    public int getTotalTransactions() {
        return totalTransactions;
    }
    
    public long getLastTradeDate() {
        return lastTradeDate;
    }
    
    // Setters
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public void setTaxRate(double taxRate) {
        this.taxRate = Math.max(0, Math.min(1, taxRate));
    }
    
    public void setExpiryDate(long expiryDate) {
        this.expiryDate = expiryDate;
    }
    
    public void addTradeVolume(double amount) {
        this.totalVolume += Math.max(0, amount);
        this.totalTransactions++;
        this.lastTradeDate = System.currentTimeMillis();
    }
    
    /**
     * Set export tariff for a specific item type
     */
    public void setExportTariff(String itemType, double tariffRate) {
        exportTariffs.put(itemType.toLowerCase(), Math.max(0, Math.min(1, tariffRate)));
    }
    
    /**
     * Set import tariff for a specific item type
     */
    public void setImportTariff(String itemType, double tariffRate) {
        importTariffs.put(itemType.toLowerCase(), Math.max(0, Math.min(1, tariffRate)));
    }
    
    /**
     * Set quota for a specific item type
     */
    public void setQuota(String itemType, int maxQuantity) {
        quotas.put(itemType.toLowerCase(), Math.max(0, maxQuantity));
    }
    
    /**
     * Get export tariff for an item type
     */
    public double getExportTariff(String itemType) {
        return exportTariffs.getOrDefault(itemType.toLowerCase(), taxRate);
    }
    
    /**
     * Get import tariff for an item type
     */
    public double getImportTariff(String itemType) {
        return importTariffs.getOrDefault(itemType.toLowerCase(), taxRate);
    }
    
    /**
     * Get quota for an item type
     */
    public int getQuota(String itemType) {
        return quotas.getOrDefault(itemType.toLowerCase(), Integer.MAX_VALUE);
    }
    
    /**
     * Check if this agreement involves a specific country
     */
    public boolean involvesCountry(String countryName) {
        return country1.equalsIgnoreCase(countryName) || country2.equalsIgnoreCase(countryName);
    }
    
    /**
     * Get the other country in this agreement
     */
    public String getOtherCountry(String countryName) {
        if (country1.equalsIgnoreCase(countryName)) {
            return country2;
        } else if (country2.equalsIgnoreCase(countryName)) {
            return country1;
        }
        return null;
    }
    
    /**
     * Check if agreement is expired
     */
    public boolean isExpired() {
        return System.currentTimeMillis() >= expiryDate;
    }
    
    /**
     * Get days until expiry
     */
    public long getDaysUntilExpiry() {
        long remaining = expiryDate - System.currentTimeMillis();
        return Math.max(0, remaining / 86400000L);
    }
    
    /**
     * Get days since creation
     */
    public long getDaysSinceCreation() {
        return (System.currentTimeMillis() - createdDate) / 86400000L;
    }
    
    /**
     * Get days since last trade
     */
    public long getDaysSinceLastTrade() {
        if (lastTradeDate == 0) {
            return -1; // No trades yet
        }
        return (System.currentTimeMillis() - lastTradeDate) / 86400000L;
    }
    
    /**
     * Extend agreement duration
     */
    public void extendDuration(long additionalDays) {
        this.expiryDate += additionalDays * 86400000L;
    }
    
    /**
     * Calculate trade tax for a transaction
     */
    public double calculateTradeTax(double amount, String itemType, boolean isExport) {
        double tariffRate = isExport ? getExportTariff(itemType) : getImportTariff(itemType);
        return amount * tariffRate;
    }
    
    /**
     * Get formatted agreement information
     */
    public List<String> getFormattedInfo() {
        List<String> info = new ArrayList<>();
        
        info.add(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        info.add(ChatUtils.colorize("&6&l📋 Trade Agreement: " + country1 + " ↔ " + country2));
        info.add(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        info.add(ChatUtils.colorize("&6Status: " + (isActive() ? "&aActive" : "&cInactive")));
        info.add(ChatUtils.colorize("&6Created: &e" + getDaysSinceCreation() + " days ago"));
        info.add(ChatUtils.colorize("&6Expires: &e" + getDaysUntilExpiry() + " days"));
        info.add(ChatUtils.colorize("&6Base Tax Rate: &e" + ChatUtils.formatPercentage(taxRate)));
        info.add(ChatUtils.colorize("&6Total Volume: &e" + ChatUtils.formatCurrency(totalVolume)));
        info.add(ChatUtils.colorize("&6Total Transactions: &e" + totalTransactions));
        
        if (lastTradeDate > 0) {
            info.add(ChatUtils.colorize("&6Last Trade: &e" + getDaysSinceLastTrade() + " days ago"));
        } else {
            info.add(ChatUtils.colorize("&6Last Trade: &7Never"));
        }
        
        if (!exportTariffs.isEmpty()) {
            info.add(ChatUtils.colorize("&6Export Tariffs:"));
            for (Map.Entry<String, Double> entry : exportTariffs.entrySet()) {
                info.add(ChatUtils.colorize("  &7" + entry.getKey() + ": &e" + 
                        ChatUtils.formatPercentage(entry.getValue())));
            }
        }
        
        if (!importTariffs.isEmpty()) {
            info.add(ChatUtils.colorize("&6Import Tariffs:"));
            for (Map.Entry<String, Double> entry : importTariffs.entrySet()) {
                info.add(ChatUtils.colorize("  &7" + entry.getKey() + ": &e" + 
                        ChatUtils.formatPercentage(entry.getValue())));
            }
        }
        
        if (!quotas.isEmpty()) {
            info.add(ChatUtils.colorize("&6Trade Quotas:"));
            for (Map.Entry<String, Integer> entry : quotas.entrySet()) {
                info.add(ChatUtils.colorize("  &7" + entry.getKey() + ": &e" + entry.getValue()));
            }
        }
        
        info.add(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        return info;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TradeAgreement agreement = (TradeAgreement) obj;
        return (country1.equalsIgnoreCase(agreement.country1) && country2.equalsIgnoreCase(agreement.country2)) ||
               (country1.equalsIgnoreCase(agreement.country2) && country2.equalsIgnoreCase(agreement.country1));
    }
    
    @Override
    public int hashCode() {
        // Ensure consistent hash regardless of country order
        String lower1 = country1.toLowerCase();
        String lower2 = country2.toLowerCase();
        if (lower1.compareTo(lower2) > 0) {
            return (lower2 + lower1).hashCode();
        } else {
            return (lower1 + lower2).hashCode();
        }
    }
    
    @Override
    public String toString() {
        return String.format("TradeAgreement{%s ↔ %s: %s, Volume: %.2f}", 
                           country1, country2, isActive() ? "Active" : "Inactive", totalVolume);
    }
}