package at.haha007.edenclient.mods;

import at.haha007.edenclient.Mod;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;

@Mod
public class AutoSheer {
    @ConfigSubscriber("false")
    boolean enabled = false;

    public AutoSheer() {
        PlayerTickCallback.EVENT.register(this::onTick);
        PerWorldConfig.get().register(this, "autoShear");
        registerCommand();
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientSuggestionProvider> node = literal("eautoshear");
        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            PlayerUtils.sendModMessage(ChatColor.GOLD + (enabled ? "AutoShear enabled" : "AutoShear disabled"));
            return 1;
        }));
        register(node,
                "AutoShear automatically shears all sheep in normal reach distance.");
    }

    private void onTick(LocalPlayer player) {
        if (!enabled) return;
        MultiPlayerGameMode interactionManager = Minecraft.getInstance().gameMode;
        if (interactionManager == null) return;
        Vec3 pos = player.position();
        Inventory inv = player.getInventory();
        InteractionHand shearHand;
        if (inv.getSelected().getItem() == Items.SHEARS)
            shearHand = InteractionHand.MAIN_HAND;
        else if (inv.offhand.get(0).getItem() == Items.SHEARS)
            shearHand = InteractionHand.OFF_HAND;
        else
            return;
        if (shearHand == InteractionHand.MAIN_HAND) {
            player.clientLevel.getEntitiesOfClass(Sheep.class, player.getBoundingBox().inflate(5), Sheep::readyForShearing).forEach(sheep -> {
                if (!sheep.readyForShearing()) return;
                if (sheep.position().distanceToSqr(pos) < 25) {
                    interactionManager.interact(player, sheep, InteractionHand.MAIN_HAND);
                }
            });
        } else {
            player.clientLevel.getEntitiesOfClass(Sheep.class, player.getBoundingBox().inflate(5), Sheep::readyForShearing).forEach(sheep -> {
                if (!sheep.readyForShearing()) return;
                if (sheep.position().distanceToSqr(pos) < 25) {
                    interactionManager.interact(player, sheep, InteractionHand.MAIN_HAND);
                    interactionManager.interact(player, sheep, InteractionHand.OFF_HAND);
                }
            });
        }
    }
}
