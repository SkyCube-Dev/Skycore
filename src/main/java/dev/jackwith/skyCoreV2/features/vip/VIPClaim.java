package dev.jackwith.skyCoreV2.features.vip;

import com.google.gson.annotations.SerializedName;

public class VIPClaim {

    @SerializedName("username")
    private String username;

    @SerializedName("rank")
    private String rank;

    @SerializedName("claimedDate")
    private String claimedDate;

    @SerializedName("grantDate")
    private String grantDate;

    @SerializedName("duration")
    private String duration;

    @SerializedName("daysGranted")
    private int daysGranted;

    public VIPClaim(String username, String rank, String claimedDate, String grantDate, String duration, int daysGranted) {
        this.username = username;
        this.rank = rank;
        this.claimedDate = claimedDate;
        this.grantDate = grantDate;
        this.duration = duration;
        this.daysGranted = daysGranted;
    }

    // Getters
    public String getUsername() { return username; }
    public String getRank() { return rank; }
    public String getClaimedDate() { return claimedDate; }
    public String getGrantDate() { return grantDate; }
    public String getDuration() { return duration; }
    public int getDaysGranted() { return daysGranted; }

    @Override
    public String toString() {
        return String.format("VIPClaim{username='%s', rank='%s', claimed='%s', days=%d}",
                username, rank, claimedDate, daysGranted);
    }
}