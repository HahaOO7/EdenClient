package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.command.Command;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.passive.SheepEntity;
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
        CommandManager.registerCommand(new Command(this::onCommand), "AutoShear");
    }

    private ActionResult onLoad(NbtCompound nbtCompound) {
        if (!nbtCompound.contains("AutoShear")) {
            enabled = false;
            return ActionResult.PASS;
        }
        NbtCompound tag = nbtCompound.getCompound("AutoShear");
        if (!tag.contains("enabled")) {
            enabled = false;
            return ActionResult.PASS;
        }
        enabled = tag.getBoolean("enabled");
        return ActionResult.PASS;
    }

    private ActionResult onSave(NbtCompound nbtCompound) {
        var tag = new NbtCompound();
        tag.putBoolean("enabled", enabled);
        return ActionResult.PASS;
    }

    private void onCommand(Command command, String s, String[] strings) {
        PlayerUtils.sendModMessage(new LiteralText((enabled = !enabled) ? "AutoShear enabled" : "AutoShear disabled").formatted(Formatting.GOLD));
    }

    private ActionResult onTick(ClientPlayerEntity player) {
        if (!enabled) return ActionResult.PASS;
        ClientPlayerInteractionManager interactionManager = MinecraftClient.getInstance().interactionManager;
        if (interactionManager == null) return ActionResult.PASS;
        Vec3d pos = player.getPos();
        Hand hand;
        if (player.getInventory().getMainHandStack().getItem() == Items.SHEARS)
            hand = Hand.MAIN_HAND;
        else if (player.getInventory().offHand.get(0).getItem() == Items.SHEARS)
            hand = Hand.OFF_HAND;
        else
            return ActionResult.PASS;
        player.clientWorld.getEntities().forEach(e -> {
            if (!(e instanceof SheepEntity sheep))
                return;
            if (!sheep.isShearable()) return;
            if (e.getPos().squaredDistanceTo(pos) < 25) {
                interactionManager.interactEntity(player, e, hand);
            }
        });
        return ActionResult.PASS;
    }
}
