package dev.jackwith.skyCoreV2.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;

import dev.jackwith.skyCoreV2.features.sales.gui.SellGui;
import org.bukkit.entity.Player;

@CommandAlias("sellgui|sellg|sell")
public class SellCommand extends BaseCommand {

    @Default
    public void openSellGui(Player player) {
        player.openInventory(new SellGui().getInventory());
    }
}