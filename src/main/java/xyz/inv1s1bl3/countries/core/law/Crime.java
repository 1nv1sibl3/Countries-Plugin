package xyz.inv1s1bl3.countries.core.law;

import java.util.UUID;

/**
 * Represents a crime committed by a player.
 */
public class Crime {
    
    private final int crimeId;
    private final UUID criminalUUID;
    private final String criminalName;
    private final UUID victimUUID; // null for victimless crimes
    private final String victimName;
    private final CrimeType crimeType;
    private final String countryName;
    private final long crimeDate;
    private final String description;
    private final String evidence;
    
    // Legal status
    private boolean investigated;
    private boolean arrested;
    private boolean convicted;
    private double fineAmount;
    private int jailTime; // in minutes
    private long arrestDate;
    private long releaseDate;
    private UUID arrestingOfficer;
    
    public Crime(int crimeId, UUID criminalUUID, String criminalName, 
                UUID victimUUID, String victimName, CrimeType crimeType, 
                String countryName, String description, String evidence) {
        this.crimeId = crimeId;
        this.criminalUUID = criminalUUID;
        this.criminalName = criminalName;
        this.victimUUID = victimUUID;
        this.victimName = victimName;
        this.crimeType = crimeType;
        this.countryName = countryName;
        this.crimeDate = System.currentTimeMillis();
        this.description = description != null ? description : "";
        this.evidence = evidence != null ? evidence : "";
        
        // Initialize legal status
        this.investigated = false;
        this.arrested = false;
        this.convicted = false;
        this.fineAmount = crimeType.getBaseFine();
        this.jailTime = crimeType.getBaseJailTime();
        this.arrestDate = 0L;
        this.releaseDate = 0L;
        this.arrestingOfficer = null;
    }
    
    // Getters
    public int getCrimeId() {
        return crimeId;
    }
    
    public UUID getCriminalUUID() {
        return criminalUUID;
    }
    
    public String getCriminalName() {
        return criminalName;
    }
    
    public UUID getVictimUUID() {
        return victimUUID;
    }
    
    public String getVictimName() {
        return victimName;
    }
    
    public CrimeType getCrimeType() {
        return crimeType;
    }
    
    public String getCountryName() {
        return countryName;
    }
    
    public long getCrimeDate() {
        return crimeDate;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getEvidence() {
        return evidence;
    }
    
    public boolean isInvestigated() {
        return investigated;
    }
    
    public boolean isArrested() {
        return arrested;
    }
    
    public boolean isConvicted() {
        return convicted;
    }
    
    public double getFineAmount() {
        return fineAmount;
    }
    
    public int getJailTime() {
        return jailTime;
    }
    
    public long getArrestDate() {
        return arrestDate;
    }
    
    public long getReleaseDate() {
        return releaseDate;
    }
    
    public UUID getArrestingOfficer() {
        return arrestingOfficer;
    }
    
    // Setters
    public void setInvestigated(boolean investigated) {
        this.investigated = investigated;
    }
    
    public void setArrested(boolean arrested) {
        this.arrested = arrested;
        if (arrested) {
            this.arrestDate = System.currentTimeMillis();
        }
    }
    
    public void setConvicted(boolean convicted) {
        this.convicted = convicted;
    }
    
    public void setFineAmount(double fineAmount) {
        this.fineAmount = Math.max(0, fineAmount);
    }
    
    public void setJailTime(int jailTime) {
        this.jailTime = Math.max(0, jailTime);
    }
    
    public void setReleaseDate(long releaseDate) {
        this.releaseDate = releaseDate;
    }
    
    public void setArrestingOfficer(UUID arrestingOfficer) {
        this.arrestingOfficer = arrestingOfficer;
    }
    
    /**
     * Check if the criminal is currently in jail
     */
    public boolean isInJail() {
        return arrested && releaseDate > System.currentTimeMillis();
    }
    
    /**
     * Get remaining jail time in minutes
     */
    public long getRemainingJailTime() {
        if (!isInJail()) {
            return 0;
        }
        return (releaseDate - System.currentTimeMillis()) / 60000; // Convert to minutes
    }
    
    /**
     * Get days since crime was committed
     */
    public long getDaysSinceCrime() {
        return (System.currentTimeMillis() - crimeDate) / 86400000;
    }
    
    /**
     * Check if crime has a victim
     */
    public boolean hasVictim() {
        return victimUUID != null;
    }
    
    /**
     * Get crime status display
     */
    public String getStatusDisplay() {
        if (convicted) {
            return "&cConvicted";
        } else if (arrested) {
            return "&6Arrested";
        } else if (investigated) {
            return "&eInvestigated";
        } else {
            return "&7Reported";
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Crime crime = (Crime) obj;
        return crimeId == crime.crimeId;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(crimeId);
    }
    
    @Override
    public String toString() {
        return String.format("Crime{id=%d, type=%s, criminal=%s, status=%s}", 
                           crimeId, crimeType.getDisplayName(), criminalName, 
                           convicted ? "Convicted" : arrested ? "Arrested" : "Reported");
    }
}