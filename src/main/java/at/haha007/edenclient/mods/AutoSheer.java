package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class AutoSheer {
    @ConfigSubscriber("false")
    boolean enabled = false;

    public AutoSheer() {
        PlayerTickCallback.EVENT.register(this::onTick);
        PerWorldConfig.get().register(this, "autoShear");
        registerCommand();
    }

    private void registerCommand() {
        CommandManager.register(CommandManager.literal("eautoshear").executes(c -> {
            enabled = !enabled;
            PlayerUtils.sendModMessage(new LiteralText(enabled ? "AutoShear enabled" : "AutoShear disabled").formatted(Formatting.GOLD));
            return 1;
        }));
    }

    private void onTick(ClientPlayerEntity player) {
        if (!enabled) return;
        ClientPlayerInteractionManager interactionManager = MinecraftClient.getInstance().interactionManager;
        if (interactionManager == null) return;
        Vec3d pos = player.getPos();
        PlayerInventory inv = player.getInventory();
        Hand shearHand;
        if (inv.getMainHandStack().getItem() == Items.SHEARS)
            shearHand = Hand.MAIN_HAND;
        else if (inv.offHand.get(0).getItem() == Items.SHEARS)
            shearHand = Hand.OFF_HAND;
        else
            return;
        if (shearHand == Hand.MAIN_HAND) {
            player.clientWorld.getEntitiesByClass(SheepEntity.class, player.getBoundingBox().expand(5), SheepEntity::isShearable).forEach(sheep -> {
                if (!sheep.isShearable()) return;
                if (sheep.getPos().squaredDistanceTo(pos) < 25) {
                    interactionManager.interactEntity(player, sheep, Hand.MAIN_HAND);
                }
            });
        } else {
            player.clientWorld.getEntitiesByClass(SheepEntity.class, player.getBoundingBox().expand(5), SheepEntity::isShearable).forEach(sheep -> {
                if (!sheep.isShearable()) return;
                if (sheep.getPos().squaredDistanceTo(pos) < 25) {
                    interactionManager.interactEntity(player, sheep, Hand.MAIN_HAND);
                    interactionManager.interactEntity(player, sheep, Hand.OFF_HAND);
                }
            });
        }
    }
}
