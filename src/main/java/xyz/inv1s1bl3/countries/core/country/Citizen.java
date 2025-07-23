package xyz.inv1s1bl3.countries.core.country;

import java.util.UUID;

/**
 * Represents a citizen of a country with their role and information.
 */
public class Citizen {
    
    private final UUID playerUUID;
    private final String playerName;
    private CitizenRole role;
    private final long joinedDate;
    private double salary;
    private long lastSalaryPaid;
    
    public Citizen(UUID playerUUID, String playerName, CitizenRole role) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.role = role;
        this.joinedDate = System.currentTimeMillis();
        this.salary = 0.0;
        this.lastSalaryPaid = 0L;
    }
    
    public Citizen(UUID playerUUID, String playerName, CitizenRole role, 
                   long joinedDate, double salary, long lastSalaryPaid) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.role = role;
        this.joinedDate = joinedDate;
        this.salary = salary;
        this.lastSalaryPaid = lastSalaryPaid;
    }
    
    // Getters
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public CitizenRole getRole() {
        return role;
    }
    
    public long getJoinedDate() {
        return joinedDate;
    }
    
    public double getSalary() {
        return salary;
    }
    
    public long getLastSalaryPaid() {
        return lastSalaryPaid;
    }
    
    // Setters
    public void setRole(CitizenRole role) {
        this.role = role;
    }
    
    public void setSalary(double salary) {
        this.salary = Math.max(0, salary);
    }
    
    public void setLastSalaryPaid(long lastSalaryPaid) {
        this.lastSalaryPaid = lastSalaryPaid;
    }
    
    /**
     * Check if this citizen can perform an action on another citizen
     */
    public boolean canManage(Citizen other) {
        return this.role.canManageRole(other.role);
    }
    
    /**
     * Check if salary is due (24 hours since last payment)
     */
    public boolean isSalaryDue() {
        return salary > 0 && 
               (System.currentTimeMillis() - lastSalaryPaid) >= 86400000; // 24 hours
    }
    
    /**
     * Get days since joining
     */
    public long getDaysSinceJoining() {
        return (System.currentTimeMillis() - joinedDate) / 86400000; // Convert to days
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Citizen citizen = (Citizen) obj;
        return playerUUID.equals(citizen.playerUUID);
    }
    
    @Override
    public int hashCode() {
        return playerUUID.hashCode();
    }
    
    @Override
    public String toString() {
        return String.format("Citizen{name=%s, role=%s, joined=%d}", 
                           playerName, role.getDisplayName(), joinedDate);
    }
}