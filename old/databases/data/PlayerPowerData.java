package dev.jackwith.skyCoreV2.databases.data;

import java.util.UUID;

public class PlayerPowerData {
    private final UUID uuid;
    private String power;
    private String unlockedPower;
    private boolean usedFreePower;

    public PlayerPowerData(UUID uuid, String power, String unlockedPower, boolean usedFreePower) {
        this.uuid = uuid;
        this.power = power;
        this.unlockedPower = unlockedPower;
        this.usedFreePower = usedFreePower;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public String getPower() {
        return this.power;
    }

    public String getUnlockedPower() {
        return this.unlockedPower;
    }

    public boolean hasUsedFreePower() {
        return this.usedFreePower;
    }

    public void setPower(String power) {
        this.power = power;
    }

    public void setUnlockedPower(String unlockedPower) {
        this.unlockedPower = unlockedPower;
    }

    public void setUsedFreePower(boolean usedFreePower) {
        this.usedFreePower = usedFreePower;
    }
}