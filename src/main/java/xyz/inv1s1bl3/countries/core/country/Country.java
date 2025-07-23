package xyz.inv1s1bl3.countries.core.country;

import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a country with its citizens, territories, and properties.
 */
public class Country {
    
    private final String name;
    private final UUID ownerUUID;
    private final long foundedDate;
    private GovernmentType governmentType;
    private double balance;
    private double taxRate;
    private String description;
    private String flag;
    
    // Citizens management
    private final Map<UUID, Citizen> citizens;
    private final Set<UUID> invitations;
    
    // Settings
    private boolean openBorders;
    private boolean allowForeigners;
    private int maxCitizens;
    
    // Statistics
    private long lastActive;
    private int totalTerritories;
    
    public Country(String name, UUID ownerUUID, GovernmentType governmentType) {
        this.name = name;
        this.ownerUUID = ownerUUID;
        this.foundedDate = System.currentTimeMillis();
        this.governmentType = governmentType;
        this.balance = 0.0;
        this.taxRate = 0.05; // 5% default
        this.description = "A newly founded country.";
        this.flag = "🏴";
        
        this.citizens = new ConcurrentHashMap<>();
        this.invitations = ConcurrentHashMap.newKeySet();
        
        this.openBorders = false;
        this.allowForeigners = true;
        this.maxCitizens = 100;
        
        this.lastActive = System.currentTimeMillis();
        this.totalTerritories = 0;
    }
    
    // Basic getters
    public String getName() {
        return name;
    }
    
    public UUID getOwnerUUID() {
        return ownerUUID;
    }
    
    public long getFoundedDate() {
        return foundedDate;
    }
    
    public GovernmentType getGovernmentType() {
        return governmentType;
    }
    
    public double getBalance() {
        return balance;
    }
    
    public double getTaxRate() {
        return taxRate;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getFlag() {
        return flag;
    }
    
    public boolean hasOpenBorders() {
        return openBorders;
    }
    
    public boolean allowsForeigners() {
        return allowForeigners;
    }
    
    public int getMaxCitizens() {
        return maxCitizens;
    }
    
    public long getLastActive() {
        return lastActive;
    }
    
    public int getTotalTerritories() {
        return totalTerritories;
    }
    
    // Setters
    public void setGovernmentType(GovernmentType governmentType) {
        this.governmentType = governmentType;
        updateLastActive();
    }
    
    public void setBalance(double balance) {
        this.balance = Math.max(0, balance);
    }
    
    public void setTaxRate(double taxRate) {
        this.taxRate = Math.max(0, Math.min(1, taxRate));
        updateLastActive();
    }
    
    public void setDescription(String description) {
        this.description = description != null ? description : "A country.";
        updateLastActive();
    }
    
    public void setFlag(String flag) {
        this.flag = flag != null ? flag : "🏴";
        updateLastActive();
    }
    
    public void setOpenBorders(boolean openBorders) {
        this.openBorders = openBorders;
        updateLastActive();
    }
    
    public void setAllowForeigners(boolean allowForeigners) {
        this.allowForeigners = allowForeigners;
        updateLastActive();
    }
    
    public void setMaxCitizens(int maxCitizens) {
        this.maxCitizens = Math.max(1, maxCitizens);
        updateLastActive();
    }
    
    public void setTotalTerritories(int totalTerritories) {
        this.totalTerritories = Math.max(0, totalTerritories);
    }
    
    // Citizen management
    public Collection<Citizen> getCitizens() {
        return citizens.values();
    }
    
    public Citizen getCitizen(UUID playerUUID) {
        return citizens.get(playerUUID);
    }
    
    public boolean isCitizen(UUID playerUUID) {
        return citizens.containsKey(playerUUID);
    }
    
    public boolean isOwner(UUID playerUUID) {
        return ownerUUID.equals(playerUUID);
    }
    
    public int getCitizenCount() {
        return citizens.size();
    }
    
    public boolean canAddCitizen() {
        return citizens.size() < maxCitizens;
    }
    
    /**
     * Add a citizen to the country
     */
    public boolean addCitizen(UUID playerUUID, String playerName, CitizenRole role) {
        if (citizens.containsKey(playerUUID) || !canAddCitizen()) {
            return false;
        }
        
        Citizen citizen = new Citizen(playerUUID, playerName, role);
        citizens.put(playerUUID, citizen);
        invitations.remove(playerUUID); // Remove any pending invitation
        updateLastActive();
        return true;
    }
    
    /**
     * Remove a citizen from the country
     */
    public boolean removeCitizen(UUID playerUUID) {
        if (isOwner(playerUUID)) {
            return false; // Cannot remove owner
        }
        
        boolean removed = citizens.remove(playerUUID) != null;
        if (removed) {
            updateLastActive();
        }
        return removed;
    }
    
    /**
     * Promote a citizen to a higher role
     */
    public boolean promoteCitizen(UUID playerUUID) {
        Citizen citizen = citizens.get(playerUUID);
        if (citizen == null || citizen.getRole() == CitizenRole.OWNER) {
            return false;
        }
        
        CitizenRole newRole = citizen.getRole().getHigherRole();
        if (newRole != citizen.getRole()) {
            citizen.setRole(newRole);
            updateLastActive();
            return true;
        }
        return false;
    }
    
    /**
     * Demote a citizen to a lower role
     */
    public boolean demoteCitizen(UUID playerUUID) {
        Citizen citizen = citizens.get(playerUUID);
        if (citizen == null || citizen.getRole() == CitizenRole.GUEST) {
            return false;
        }
        
        CitizenRole newRole = citizen.getRole().getLowerRole();
        if (newRole != citizen.getRole()) {
            citizen.setRole(newRole);
            updateLastActive();
            return true;
        }
        return false;
    }
    
    // Invitation management
    public Set<UUID> getInvitations() {
        return new HashSet<>(invitations);
    }
    
    public boolean hasInvitation(UUID playerUUID) {
        return invitations.contains(playerUUID);
    }
    
    public void addInvitation(UUID playerUUID) {
        invitations.add(playerUUID);
    }
    
    public void removeInvitation(UUID playerUUID) {
        invitations.remove(playerUUID);
    }
    
    // Economy methods
    public boolean hasBalance(double amount) {
        return balance >= amount;
    }
    
    public boolean withdraw(double amount) {
        if (hasBalance(amount)) {
            balance -= amount;
            return true;
        }
        return false;
    }
    
    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
        }
    }
    
    /**
     * Calculate total tax income from all citizens
     */
    public double calculateTaxIncome() {
        // This would integrate with the economy system
        // For now, return a placeholder
        return citizens.size() * 100 * taxRate;
    }
    
    /**
     * Get citizens who are due for salary payment
     */
    public List<Citizen> getCitizensDueForSalary() {
        return citizens.values().stream()
                .filter(Citizen::isSalaryDue)
                .toList();
    }
    
    // Utility methods
    private void updateLastActive() {
        this.lastActive = System.currentTimeMillis();
    }
    
    public long getDaysSinceFoundation() {
        return (System.currentTimeMillis() - foundedDate) / 86400000;
    }
    
    public boolean isActive() {
        // Consider country active if used within last 7 days
        return (System.currentTimeMillis() - lastActive) < 604800000;
    }
    
    /**
     * Get formatted country information
     */
    public List<String> getFormattedInfo() {
        List<String> info = new ArrayList<>();
        
        info.add(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        info.add(ChatUtils.colorize("&6&l" + flag + " " + name + " " + flag));
        info.add(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        info.add(ChatUtils.colorize("&6Government: &e" + governmentType.getDisplayName()));
        info.add(ChatUtils.colorize("&6Founded: &e" + getDaysSinceFoundation() + " days ago"));
        info.add(ChatUtils.colorize("&6Citizens: &e" + citizens.size() + "/" + maxCitizens));
        info.add(ChatUtils.colorize("&6Territories: &e" + totalTerritories));
        info.add(ChatUtils.colorize("&6Treasury: &e" + ChatUtils.formatCurrency(balance)));
        info.add(ChatUtils.colorize("&6Tax Rate: &e" + ChatUtils.formatPercentage(taxRate)));
        info.add(ChatUtils.colorize("&6Status: " + (isActive() ? "&aActive" : "&cInactive")));
        info.add(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        if (!description.isEmpty()) {
            info.add(ChatUtils.colorize("&7" + description));
            info.add(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        }
        
        return info;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Country country = (Country) obj;
        return name.equals(country.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    @Override
    public String toString() {
        return String.format("Country{name=%s, citizens=%d, territories=%d}", 
                           name, citizens.size(), totalTerritories);
    }
}