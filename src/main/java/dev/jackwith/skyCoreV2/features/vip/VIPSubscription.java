package dev.jackwith.skyCoreV2.features.vip;

import com.google.gson.annotations.SerializedName;

public class VIPSubscription {

    @SerializedName("username")
    private String username;

    @SerializedName("rank")
    private String rank;

    @SerializedName("grantDate")
    private String grantDate;

    @SerializedName("duration")
    private int duration;

    @SerializedName("expiryDate")
    private String expiryDate;

    @SerializedName("bonusExpiryDate")
    private String bonusExpiryDate;

    @SerializedName("daysRemaining")
    private int daysRemaining;

    @SerializedName("isActive")
    private boolean isActive;

    @SerializedName("status")
    private String status;

    @SerializedName("context")
    private String context;

    @SerializedName("sourceFile")
    private String sourceFile;

    @SerializedName("timestamp")
    private String timestamp;

    // Getters
    public String getUsername() { return username; }
    public String getRank() { return rank; }
    public String getGrantDate() { return grantDate; }
    public int getDuration() { return duration; }
    public String getExpiryDate() { return expiryDate; }
    public String getBonusExpiryDate() { return bonusExpiryDate; }
    public int getDaysRemaining() { return daysRemaining; }
    public boolean isActive() { return isActive; }
    public String getStatus() { return status; }
    public String getContext() { return context; }
    public String getSourceFile() { return sourceFile; }
    public String getTimestamp() { return timestamp; }

    /**
     * Get formatted duration string for LuckPerms command
     * e.g., "1week2day" or "15day"
     */
    public String getFormattedDuration() {
        int days = daysRemaining;

        if (days <= 0) {
            return "1day"; // Give at least 1 day
        }

        int weeks = days / 7;
        int remainingDays = days % 7;

        StringBuilder duration = new StringBuilder();

        if (weeks > 0) {
            duration.append(weeks).append("week");
        }

        if (remainingDays > 0) {
            if (weeks > 0) duration.append(" ");
            duration.append(remainingDays).append("day");
        }

        return duration.toString();
    }

    @Override
    public String toString() {
        return String.format("VIPSubscription{username='%s', rank='%s', daysRemaining=%d, active=%b}",
                username, rank, daysRemaining, isActive);
    }
}