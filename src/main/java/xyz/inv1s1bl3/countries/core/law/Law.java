package xyz.inv1s1bl3.countries.core.law;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a law within a country's legal system.
 */
public class Law {
    
    private final int lawId;
    private final String countryName;
    private String title;
    private String description;
    private final long createdDate;
    private long lastModified;
    private boolean active;
    private double fineAmount;
    private int jailTime; // in minutes
    private final List<CrimeType> applicableCrimes;
    
    public Law(int lawId, String countryName, String title, String description) {
        this.lawId = lawId;
        this.countryName = countryName;
        this.title = title;
        this.description = description;
        this.createdDate = System.currentTimeMillis();
        this.lastModified = createdDate;
        this.active = true;
        this.fineAmount = 100.0;
        this.jailTime = 5;
        this.applicableCrimes = new ArrayList<>();
    }
    
    public Law(int lawId, String countryName, String title, String description,
              long createdDate, long lastModified, boolean active, 
              double fineAmount, int jailTime) {
        this.lawId = lawId;
        this.countryName = countryName;
        this.title = title;
        this.description = description;
        this.createdDate = createdDate;
        this.lastModified = lastModified;
        this.active = active;
        this.fineAmount = fineAmount;
        this.jailTime = jailTime;
        this.applicableCrimes = new ArrayList<>();
    }
    
    // Getters
    public int getLawId() {
        return lawId;
    }
    
    public String getCountryName() {
        return countryName;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public long getCreatedDate() {
        return createdDate;
    }
    
    public long getLastModified() {
        return lastModified;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public double getFineAmount() {
        return fineAmount;
    }
    
    public int getJailTime() {
        return jailTime;
    }
    
    public List<CrimeType> getApplicableCrimes() {
        return new ArrayList<>(applicableCrimes);
    }
    
    // Setters
    public void setTitle(String title) {
        this.title = title;
        this.lastModified = System.currentTimeMillis();
    }
    
    public void setDescription(String description) {
        this.description = description;
        this.lastModified = System.currentTimeMillis();
    }
    
    public void setActive(boolean active) {
        this.active = active;
        this.lastModified = System.currentTimeMillis();
    }
    
    public void setFineAmount(double fineAmount) {
        this.fineAmount = Math.max(0, fineAmount);
        this.lastModified = System.currentTimeMillis();
    }
    
    public void setJailTime(int jailTime) {
        this.jailTime = Math.max(0, jailTime);
        this.lastModified = System.currentTimeMillis();
    }
    
    /**
     * Add applicable crime type
     */
    public void addApplicableCrime(CrimeType crimeType) {
        if (!applicableCrimes.contains(crimeType)) {
            applicableCrimes.add(crimeType);
            this.lastModified = System.currentTimeMillis();
        }
    }
    
    /**
     * Remove applicable crime type
     */
    public void removeApplicableCrime(CrimeType crimeType) {
        if (applicableCrimes.remove(crimeType)) {
            this.lastModified = System.currentTimeMillis();
        }
    }
    
    /**
     * Check if law applies to a specific crime type
     */
    public boolean appliesTo(CrimeType crimeType) {
        return active && (applicableCrimes.isEmpty() || applicableCrimes.contains(crimeType));
    }
    
    /**
     * Get days since law was created
     */
    public long getDaysSinceCreation() {
        return (System.currentTimeMillis() - createdDate) / 86400000;
    }
    
    /**
     * Get days since last modification
     */
    public long getDaysSinceModification() {
        return (System.currentTimeMillis() - lastModified) / 86400000;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Law law = (Law) obj;
        return lawId == law.lawId;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(lawId);
    }
    
    @Override
    public String toString() {
        return String.format("Law{id=%d, title=%s, country=%s, active=%s}", 
                           lawId, title, countryName, active);
    }
}