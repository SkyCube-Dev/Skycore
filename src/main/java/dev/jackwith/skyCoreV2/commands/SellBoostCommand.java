package dev.jackwith.skyCoreV2.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.databases.SalesDB;
import dev.jackwith.skyCoreV2.utils.Lang;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

@CommandAlias("sellboost|sb")
public class SellBoostCommand extends BaseCommand {

    private final SalesDB salesDB;

    public SellBoostCommand() {
        this.salesDB = SkyCore.getInstance().getsalesDB();
    }

    @Default
    public void DefaultCommand(Player player) {
        double boost = salesDB.getPlayerSellBoost(player.getUniqueId());
        player.sendMessage(Lang.getColored("<gray>(<green>/sellboost<gray>) | <white>Your current sell boost: <green>" + boost + "x"));
    }

    @Subcommand("check")
    @CommandPermission("skycore.admin.sellboost")
    @CommandCompletion("@players")
    public void Ccheck(CommandSender sender, String targetName) {
        Player target = SkyCore.getInstance().getServer().getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(Lang.getColored("<gray>(<green>/sellboost<gray>) | <red>Player not found."));
            return;
        }

        double boost = salesDB.getPlayerSellBoost(target.getUniqueId());
        sender.sendMessage(Lang.getColored("<gray>(<green>/sellboost<gray>) | <white>" + target.getName() + "'s sell boost: <green>" + boost + "x"));
    }

    @Subcommand("set")
    @CommandPermission("skycore.admin.sellboost")
    @CommandCompletion("@players")
    public void setboost(CommandSender sender, String targetName, double value) {
        Player target = SkyCore.getInstance().getServer().getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(Lang.getColored("<gray>(<green>/sellboost<gray>) | <red>Player not found."));
            return;
        }

        salesDB.setPlayerSellBoost(target.getUniqueId(), value);
        sender.sendMessage(Lang.getColored("<gray>(<green>/sellboost<gray>) | Set <white>" + target.getName() + "'s sell boost to <green>" + value + "x"));
        target.sendMessage(Lang.getColored("<gray>(<green>/sellboost<gray>) | Your sell boost has been set to <green>" + value + "x"));
    }

    @Subcommand("add")
    @CommandPermission("skycore.admin.sellboost")
    @CommandCompletion("@players")
    public void addboost(CommandSender sender, String targetName, double value) {
        Player target = SkyCore.getInstance().getServer().getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(Lang.getColored("<gray>(<green>/sellboost<gray>) | <red>Player not found."));
            return;
        }

        UUID uuid = target.getUniqueId();
        double newBoost = salesDB.getPlayerSellBoost(uuid) + value;
        salesDB.setPlayerSellBoost(uuid, newBoost);

        sender.sendMessage(Lang.getColored("<gray>(<green>/sellboost<gray>) | Added <green>" + value + "x <white>to " + target.getName() + " (Total: <green>" + newBoost + "x<white>)"));
        target.sendMessage(Lang.getColored("<gray>(<green>/sellboost<gray>) | Your sell boost is now <green>" + newBoost + "x"));
    }

    @Subcommand("remove|del")
    @CommandPermission("skycore.admin.sellboost")
    @CommandCompletion("@players")
    public void removeboost(CommandSender sender, String targetName, double value) {
        Player target = SkyCore.getInstance().getServer().getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(Lang.getColored("<gray>(<green>/sellboost<gray>) | <red>Player not found."));
            return;
        }

        UUID uuid = target.getUniqueId();
        double newBoost = Math.max(1.0, salesDB.getPlayerSellBoost(uuid) - value);
        salesDB.setPlayerSellBoost(uuid, newBoost);

        sender.sendMessage(Lang.getColored("<gray>(<green>/sellboost<gray>) | Removed <green>" + value + "x <white>from " + target.getName() + " (Total: <green>" + newBoost + "x<white>)"));
        target.sendMessage(Lang.getColored("<gray>(<green>/sellboost<gray>) | Your sell boost is now <green>" + newBoost + "x"));
    }

    @Subcommand("reset")
    @CommandPermission("skycore.admin.sellboost")
    @CommandCompletion("@players")
    public void resetboost(CommandSender sender, String targetName) {
        Player target = SkyCore.getInstance().getServer().getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(Lang.getColored("<gray>(<green>/sellboost<gray>) | <red>Player not found."));
            return;
        }

        salesDB.setPlayerSellBoost(target.getUniqueId(), 1.0);
        sender.sendMessage(Lang.getColored("<gray>(<green>/sellboost<gray>) | Reset <white>" + target.getName() + "'s sell boost to <green>1.0x"));
        target.sendMessage(Lang.getColored("<gray>(<green>/sellboost<gray>) | Your sell boost has been reset to <green>1.0x"));
    }
}