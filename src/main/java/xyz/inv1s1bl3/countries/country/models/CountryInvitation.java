package xyz.inv1s1bl3.countries.country.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an invitation to join a country
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class CountryInvitation {
    
    private Integer id;
    private Integer countryId;
    private UUID inviterUuid;
    private UUID inviteeUuid;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String status;
    
    /**
     * Check if invitation is still valid
     * @return true if invitation is valid
     */
    public boolean isValid() {
        return this.status.equals("pending") && LocalDateTime.now().isBefore(this.expiresAt);
    }
    
    /**
     * Check if invitation has expired
     * @return true if invitation has expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
    
    /**
     * Check if invitation is pending
     * @return true if invitation is pending
     */
    public boolean isPending() {
        return this.status.equals("pending");
    }
    
    /**
     * Check if invitation was accepted
     * @return true if invitation was accepted
     */
    public boolean isAccepted() {
        return this.status.equals("accepted");
    }
    
    /**
     * Check if invitation was declined
     * @return true if invitation was declined
     */
    public boolean isDeclined() {
        return this.status.equals("declined");
    }
    
    /**
     * Accept the invitation
     */
    public void accept() {
        this.status = "accepted";
    }
    
    /**
     * Decline the invitation
     */
    public void decline() {
        this.status = "declined";
    }
    
    /**
     * Expire the invitation
     */
    public void expire() {
        this.status = "expired";
    }
    
    /**
     * Get time remaining until expiration
     * @return Minutes until expiration, or 0 if expired
     */
    public long getMinutesUntilExpiration() {
        if (this.isExpired()) {
            return 0;
        }
        
        final LocalDateTime now = LocalDateTime.now();
        return java.time.Duration.between(now, this.expiresAt).toMinutes();
    }
}