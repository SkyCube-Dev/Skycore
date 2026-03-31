package dev.jackwith.skyCoreV2.hooks.expansions;

import dev.jackwith.skyCoreV2.SkyCore;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PlaytimeExpansion extends PlaceholderExpansion {

    private List<String[]> leaderboardCache = new ArrayList<>();
    private long lastUpdate = 0;

    @Override
    public @NotNull String getIdentifier() { return "playtime"; }

    @Override
    public @NotNull String getAuthor() { return "SkyCore"; }

    @Override
    public @NotNull String getVersion() { return "1.0"; }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        updateCache();

        if (params.startsWith("leaderboard_player_")) {
            int index = Integer.parseInt(params.replace("leaderboard_player_", "")) - 1;
            if (index < 0 || index >= leaderboardCache.size()) return "---";

            return leaderboardCache.get(index)[0];
        }

        if (params.startsWith("leaderboard_value_")) {
            int index = Integer.parseInt(params.replace("leaderboard_value_", "")) - 1;
            if (index < 0 || index >= leaderboardCache.size()) return "0h 0m";

            return leaderboardCache.get(index)[1];
        }

        if (params.equals("leaderboard_placement")) {
            if (player == null) return "N/A";
            String targetName = player.getName();
            for (int i = 0; i < leaderboardCache.size(); i++) {
                if (leaderboardCache.get(i)[0].equals(targetName)) {
                    return String.valueOf(i + 1);
                }
            }
            return "N/A";
        }

        return null;
    }

    private void updateCache() {
        if (System.currentTimeMillis() - lastUpdate > 300_000) {
            this.leaderboardCache = SkyCore.getPlaytimeManager()
                    .getDatabase()
                    .topPlaytime(10);
            this.lastUpdate = System.currentTimeMillis();
        }
    }
}