package dev.jackwith.skyCoreV2.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.features.pets.Pet;
import dev.jackwith.skyCoreV2.features.pets.gui.PetsGUI;
import dev.jackwith.skyCoreV2.features.pets.PetService;
import dev.jackwith.skyCoreV2.utils.Lang;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

@CommandAlias("pets")
public class PetCommand extends BaseCommand {

    private final PetService service;

    public PetCommand(PetService service) {
        this.service = service;
    }

    @Default
    public void PetsGUI(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Lang.sendCommandLang(sender, "must-be-player");
            return;
        }
        player.openInventory(PetsGUI.create(player, 1, 1));
    }

    @Subcommand("reload")
    @CommandPermission("skypets.admin")
    public void PetsReload(CommandSender sender) {
        service.loadRegistry();
        Lang.sendCommandLang(sender, "reload");
    }

    @Subcommand("giveall")
    @CommandPermission("skypets.admin")
    @CommandCompletion("@players")
    public void GiveALL(CommandSender sender, @Optional OfflinePlayer target) {
        if (target == null && sender instanceof Player player) target = player;
        if (target == null) {
            Lang.sendCommandLang(sender, "must-specify-player");
            return;
        }

        int count = 0;
        for (Pet pet : service.getAllPets()) {
            if (!service.hasPet(target.getUniqueId(), pet.id())) {
                service.givePet(target.getUniqueId(), pet.id());
                count++;
            }
        }

        Lang.sendCommandLang(sender, "giveall-success", "<target>", target.getName());
    }

    @Subcommand("give")
    @CommandPermission("skypets.admin")
    @CommandCompletion("@players @pets")
    public void PetsGive(CommandSender sender, OfflinePlayer target, String petId) {
        Pet pet = service.getPet(petId);
        if (pet == null) {
            Lang.sendCommandLang(sender, "invalid-pet", "<id>", petId);
            return;
        }

        if (service.hasPet(target.getUniqueId(), pet.id())) {
            Lang.sendCommandLang(sender, "already-owns", "<target>", target.getName(), "<pet>", pet.name());
            return;
        }

        service.givePet(target.getUniqueId(), pet.id());
        Lang.sendCommandLang(sender, "give-success-sender", "<pet>", pet.name(), "<target>", target.getName());

        if (target.isOnline()) {
            Lang.sendCommandLang(target.getPlayer(), "give-success-target", "<pet>", pet.name());
        }
    }

    @Subcommand("remove")
    @CommandPermission("skypets.admin")
    @CommandCompletion("@players @pets")
    public void PetsRemove(CommandSender sender, OfflinePlayer target, String petId) {
        Pet pet = service.getPet(petId);
        if (pet == null) {
            Lang.sendCommandLang(sender, "invalid-pet", "<id>", petId);
            return;
        }

        if (!service.hasPet(target.getUniqueId(), pet.id())) {
            Lang.sendCommandLang(sender, "does-not-own", "<target>", target.getName(), "<pet>", pet.name());
            return;
        }

        service.removePet(target.getUniqueId(), pet.id());
        Lang.sendCommandLang(sender, "remove-success", "<pet>", pet.name(), "<target>", target.getName());
    }

    @Subcommand("slots")
    @CommandPermission("skypets.admin")
    @CommandCompletion("@players")
    public void PetsSlots(CommandSender sender, OfflinePlayer target) {
        int slots = SkyCore.getPetsCollection().getPetSlots(target.getUniqueId());
        Lang.sendCommandLang(sender, "pet-slots-info", "<target>", target.getName(), "<slots>", String.valueOf(slots));
    }

    @Subcommand("slots add")
    @CommandPermission("skypets.admin")
    @CommandCompletion("@players @range:1-10")
    public void PetsSlotsAdd(CommandSender sender, OfflinePlayer target, int amount) {
        if (amount <= 0) {
            Lang.sendCommandLang(sender, "invalid-amount");
            return;
        }

        UUID uuid = target.getUniqueId();
        int currentSlots = SkyCore.getPetsCollection().getPetSlots(uuid);
        int newSlots = currentSlots + amount;

        SkyCore.getPetsCollection().setPetSlots(uuid, newSlots);
        Lang.sendCommandLang(sender, "pet-slots-added", "<target>", target.getName(), "<amount>", String.valueOf(amount), "<total>", String.valueOf(newSlots));

        if (target.isOnline()) {
            Lang.sendCommandLang(target.getPlayer(), "pet-slots-received", "<amount>", String.valueOf(amount), "<total>", String.valueOf(newSlots));
        }
    }

    @Subcommand("slots set")
    @CommandPermission("skypets.admin")
    @CommandCompletion("@players @range:1-50")
    public void PetsSlotsSet(CommandSender sender, OfflinePlayer target, int slots) {
        if (slots <= 0) {
            Lang.sendCommandLang(sender, "invalid-slots");
            return;
        }

        UUID uuid = target.getUniqueId();
        SkyCore.getPetsCollection().setPetSlots(uuid, slots);
        Lang.sendCommandLang(sender, "pet-slots-set", "<target>", target.getName(), "<slots>", String.valueOf(slots));

        if (target.isOnline()) {
            Lang.sendCommandLang(target.getPlayer(), "pet-slots-set-player", "<slots>", String.valueOf(slots));
        }
    }

    @Subcommand("slots reset")
    @CommandPermission("skypets.admin")
    @CommandCompletion("@players")
    public void PetsSlotsReset(CommandSender sender, OfflinePlayer target) {
        UUID uuid = target.getUniqueId();
        SkyCore.getPetsCollection().setPetSlots(uuid, 2);
        Lang.sendCommandLang(sender, "pet-slots-reset", "<target>", target.getName());

        if (target.isOnline()) {
            Lang.sendCommandLang(target.getPlayer(), "pet-slots-reset-player");
        }
    }
}