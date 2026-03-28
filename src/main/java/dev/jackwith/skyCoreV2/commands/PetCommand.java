// /pets
// /pets reload
// /pets giveall
// /pets give

package dev.jackwith.skyCoreV2.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import dev.jackwith.skyCoreV2.features.pets.Pet;
import dev.jackwith.skyCoreV2.features.pets.gui.PetsGUI;
import dev.jackwith.skyCoreV2.features.pets.PetService;
import dev.jackwith.skyCoreV2.utils.Lang;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        player.openInventory(PetsGUI.create(player, 1));
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
            if (!service.hasPet(target.getUniqueId(), pet.getId())) {
                service.givePet(target.getUniqueId(), pet.getId());
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

        if (service.hasPet(target.getUniqueId(), pet.getId())) {
            Lang.sendCommandLang(sender, "already-owns", "<target>", target.getName(), "<pet>", pet.getName());
            return;
        }

        service.givePet(target.getUniqueId(), pet.getId());
        Lang.sendCommandLang(sender, "give-success-sender", "<pet>", pet.getName(), "<target>", target.getName());
        Lang.sendCommandLang((CommandSender) target, "give-success-target", "<pet>", pet.getName());
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

        if (!service.hasPet(target.getUniqueId(), pet.getId())) {
            Lang.sendCommandLang(sender, "does-not-own", "<target>", target.getName(), "<pet>", pet.getName());
            return;
        }

        service.removePet(target.getUniqueId(), pet.getId());
        Lang.sendCommandLang(sender, "remove-success", "<pet>", pet.getName(), "<target>", target.getName());
    }
}