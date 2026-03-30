package dev.jackwith.skyCoreV2.database.data;

public class PowerData {
    private final String id;
    private final String name;
    private final int layout;
    private final String material;
    private final int customModelData;
    private final String loreBar;
    private final String loreLine1;
    private final String loreLine2;

    public PowerData(String id, String name, int layout, String material, int customModelData, String loreBar, String loreLine1, String loreLine2) {
        this.id = id;
        this.name = name;
        this.layout = layout;
        this.material = material;
        this.customModelData = customModelData;
        this.loreBar = loreBar;
        this.loreLine1 = loreLine1;
        this.loreLine2 = loreLine2;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public int getLayout() {
        return this.layout;
    }

    public String getMaterial() {
        return this.material;
    }

    public int getCustomModelData() {
        return this.customModelData;
    }

    public String getLoreBar() {
        return this.loreBar;
    }

    public String getLoreLine1() {
        return this.loreLine1;
    }

    public String getLoreLine2() {
        return this.loreLine2;
    }
}
