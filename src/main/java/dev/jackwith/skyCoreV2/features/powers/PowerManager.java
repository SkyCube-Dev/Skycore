package dev.jackwith.skyCoreV2.features.powers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.databases.data.PowerData;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PowerManager {
    private final Map<String, PowerData> powerCache = new ConcurrentHashMap<String, PowerData>();
    private final List<PowerData> powerListView = new ArrayList<PowerData>();

    public static void applyPower(Player player, String powerKey) {
        PowerManager.removePower(player);
        if (powerKey == null || powerKey.equalsIgnoreCase("none")) {
            return;
        }
        switch (powerKey.toLowerCase()) {
            case "minion": {
                AttributeInstance scale = player.getAttribute(Attribute.SCALE);
                if (scale == null) break;
                scale.setBaseValue(0.5);
                break;
            }
            case "mario": {
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, -1, 0, false, false));
                break;
            }
            case "aquaman": {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, -1, 255, false, false));
                break;
            }
            case "superman": {
                player.setAllowFlight(true);
                break;
            }
        }
    }

    public static void removePower(Player player) {
        AttributeInstance scale = player.getAttribute(Attribute.SCALE);
        if (scale != null) {
            scale.setBaseValue(1.0);
        }
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        player.removePotionEffect(PotionEffectType.WATER_BREATHING);
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
        player.setFallDistance(0.0f);
    }

    public void loadPowers() {
        SkyCore plugin = SkyCore.getInstance();
        ConfigurationSection powers = plugin.getPowersConfig().getConfigurationSection("powers");
        if (powers == null) {
            return;
        }
        this.powerCache.clear();
        this.powerListView.clear();
        for (String key : powers.getKeys(false)) {
            ConfigurationSection section = powers.getConfigurationSection(key);
            if (section == null) continue;
            PowerData data = new PowerData(key, section.getString("name", "Unknown Power"), section.getInt("layout"), section.getString("item.material", "STONE"), section.getInt("item.custommodeldata", 0), section.getString("lore.bar", ""), section.getString("lore.line1", ""), section.getString("lore.line2", ""));
            this.powerCache.put(key, data);
            this.powerListView.add(data);
        }
    }

    public PowerData getCachedPower(String id) {
        return this.powerCache.get(id);
    }

    public List<PowerData> getCachedPowers() {
        return Collections.unmodifiableList(this.powerListView);
    }
}
