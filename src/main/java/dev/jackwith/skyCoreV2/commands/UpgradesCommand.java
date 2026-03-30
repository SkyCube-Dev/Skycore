package dev.jackwith.skyCoreV2.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.database.UpgradesCollection;
import dev.jackwith.skyCoreV2.features.upgrades.gui.UpgradesGui;
import dev.jackwith.skyCoreV2.hooks.BentoBoxHook;
import dev.jackwith.skyCoreV2.utils.Lang;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

@CommandAlias("upgrade|upgrades|expand")
public class UpgradesCommand extends BaseCommand {

    private final SkyCore plugin = SkyCore.getInstance();
    private final UpgradesCollection upgradeDB = new UpgradesCollection();

    @Default
    public void UpgradeGUI(Player player) {
        UUID islandOwnerUuid = BentoBoxHook.getIslandOwnerUUID(player);
        new UpgradesGui().open(player, islandOwnerUuid);
    }

    @Subcommand("setlevel")
    @CommandPermission("skycore.admin")
    @CommandCompletion("@players")
    public void setLevel(CommandSender sender, OfflinePlayer target, int level) {
        if (level < 0) {
            target.getPlayer().sendMessage(Lang.getColored("&aYou cannot set a negative number " + level));
            return;
        }

        upgradeDB.updateDocument(target.getUniqueId().toString(), level, 0);

        sender.sendMessage(Lang.getColored("&aSet upgrade level of " + target.getName()) + " to " + level);
        if (target.isOnline()) {
            target.getPlayer().sendMessage(Lang.getColored("&aYour upgrade level has been set to " + level));
        }
    }

}