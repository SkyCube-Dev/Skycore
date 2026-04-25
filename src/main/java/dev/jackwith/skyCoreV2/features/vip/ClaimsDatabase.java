package dev.jackwith.skyCoreV2.features.vip;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class ClaimsDatabase {

    @SerializedName("lastUpdated")
    private String lastUpdated;

    @SerializedName("totalClaims")
    private int totalClaims;

    @SerializedName("claims")
    private List<VIPClaim> claims;

    public ClaimsDatabase() {
        this.claims = new ArrayList<>();
        this.totalClaims = 0;
        this.lastUpdated = java.time.Instant.now().toString();
    }

    public String getLastUpdated() { return lastUpdated; }
    public int getTotalClaims() { return totalClaims; }
    public List<VIPClaim> getClaims() { return claims; }

    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
    public void setTotalClaims(int totalClaims) { this.totalClaims = totalClaims; }

    public void addClaim(VIPClaim claim) {
        claims.add(claim);
        totalClaims = claims.size();
        lastUpdated = java.time.Instant.now().toString();
    }

    public boolean hasClaimed(String username) {
        return claims.stream()
                .anyMatch(c -> c.getUsername().equalsIgnoreCase(username));
    }

    public VIPClaim getClaim(String username) {
        return claims.stream()
                .filter(c -> c.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
    }

    public boolean removeClaim(String username) {
        boolean removed = claims.removeIf(c -> c.getUsername().equalsIgnoreCase(username));
        if (removed) {
            totalClaims = claims.size();
            lastUpdated = java.time.Instant.now().toString();
        }
        return removed;
    }
}