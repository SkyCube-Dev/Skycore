package dev.jackwith.skyCoreV2.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.features.powers.gui.PowersGui;
import org.bukkit.entity.Player;

@CommandAlias("powers")
public class PowersCommand extends BaseCommand {

    @Default
    public void Powers(Player player) {
        PowersGui gui = new PowersGui(player);
        player.openInventory(gui.getInventory());
    }

//    @Subcommand("reload")
//    @CommandPermission("skycore.powers.admin")
//    public void PowersReload(Player player) {
//        // [TODO] Idk if this works tbh
//        SkyCore.getInstance().loadPowersConfig();
//    }

}