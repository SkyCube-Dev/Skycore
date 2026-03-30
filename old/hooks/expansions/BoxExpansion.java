package dev.jackwith.skyCoreV2.hooks.expansions;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;

import java.util.Objects;
import java.util.Optional;

public class BoxExpansion extends PlaceholderExpansion {

    @Override
    public @NotNull String getAuthor() {
        return "SkyCube";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "box";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "0";

        if (params.equalsIgnoreCase("bordersize")) {
            Optional<Island> island = Optional.ofNullable(BentoBox.getInstance().getIslands()
                    .getIsland(Objects.requireNonNull(Bukkit.getWorld("boxed_world")), player.getUniqueId()));

            if (island.isPresent()) {
                int size = island.get().getProtectionRange() * 2;
                return String.valueOf(size);
            }

            return BentoBox.getInstance().getIslands().getIslands().stream()
                    .filter(isl -> isl.getMemberSet().contains(player.getUniqueId()))
                    .findFirst()
                    .map(isl -> String.valueOf(isl.getProtectionRange() * 2))
                    .orElse("0");
        }

        return null;
    }
}