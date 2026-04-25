package dev.jackwith.skyCoreV2.features.vip;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class VIPDatabase {

    @SerializedName("metadata")
    private Metadata metadata;

    @SerializedName("users")
    private List<VIPSubscription> users;

    @SerializedName("activeUsers")
    private List<VIPSubscription> activeUsers;

    @SerializedName("expiredUsers")
    private List<VIPSubscription> expiredUsers;

    @SerializedName("expiringSoon")
    private List<VIPSubscription> expiringSoon;

    public Metadata getMetadata() { return metadata; }
    public List<VIPSubscription> getUsers() { return users; }
    public List<VIPSubscription> getActiveUsers() { return activeUsers; }
    public List<VIPSubscription> getExpiredUsers() { return expiredUsers; }
    public List<VIPSubscription> getExpiringSoon() { return expiringSoon; }

    public static class Metadata {
        @SerializedName("scanDate")
        private String scanDate;

        @SerializedName("logsDirectory")
        private String logsDirectory;

        @SerializedName("bonusDays")
        private int bonusDays;

        @SerializedName("defaultDurations")
        private Map<String, Integer> defaultDurations;

        public String getScanDate() { return scanDate; }
        public String getLogsDirectory() { return logsDirectory; }
        public int getBonusDays() { return bonusDays; }
        public Map<String, Integer> getDefaultDurations() { return defaultDurations; }
    }
}