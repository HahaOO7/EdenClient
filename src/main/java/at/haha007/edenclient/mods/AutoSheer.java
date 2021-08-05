package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class AutoSheer {
    boolean enabled = false;

    public AutoSheer() {
        PlayerTickCallback.EVENT.register(this::onTick);
        ConfigSaveCallback.EVENT.register(this::onSave);
        ConfigLoadCallback.EVENT.register(this::onLoad);
        registerCommand();
    }

    private ActionResult onLoad(NbtCompound nbtCompound) {
        if (!nbtCompound.contains("AutoShear")) {
            enabled = false;
            return ActionResult.PASS;
        }
        NbtCompound tag = nbtCompound.getCompound("AutoShear");
        enabled = tag.getBoolean("enabled");
        return ActionResult.PASS;
    }

    private ActionResult onSave(NbtCompound nbtCompound) {
        var tag = new NbtCompound();
        tag.putBoolean("enabled", enabled);
        nbtCompound.put("AutoShear", tag);
        return ActionResult.PASS;
    }

    private void registerCommand() {
        CommandManager.register(CommandManager.literal("autoshear").executes(c -> {
            enabled = !enabled;
            PlayerUtils.sendModMessage(new LiteralText(enabled ? "AutoShear enabled" : "AutoShear disabled").formatted(Formatting.GOLD));
            return 1;
        }));
    }

    private ActionResult onTick(ClientPlayerEntity player) {
        if (!enabled) return ActionResult.PASS;
        ClientPlayerInteractionManager interactionManager = MinecraftClient.getInstance().interactionManager;
        if (interactionManager == null) return ActionResult.PASS;
        Vec3d pos = player.getPos();
        PlayerInventory inv = player.getInventory();
        Hand shearHand;
        if (inv.getMainHandStack().getItem() == Items.SHEARS)
            shearHand = Hand.MAIN_HAND;
        else if (inv.offHand.get(0).getItem() == Items.SHEARS)
            shearHand = Hand.OFF_HAND;
        else
            return ActionResult.PASS;
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
        return ActionResult.PASS;
    }
}
