package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.command.Command;
import at.haha007.edenclient.command.CommandManager;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.particle.BarrierParticle;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

import static at.haha007.edenclient.utils.PlayerUtils.sendMessage;

public class BarrierDisplay {
    private boolean enabled = true;
    private static final int dist = 5;
    private final Random rand = new Random();
    private int counter = 20;

    public BarrierDisplay() {
        CommandManager.registerCommand(new Command(this::onCommand), "barrier");
        PlayerTickCallback.EVENT.register(this::onTick);
        ConfigSaveCallback.EVENT.register(this::onSave);
        ConfigLoadCallback.EVENT.register(this::onLoad);
    }


    private ActionResult onTick(ClientPlayerEntity player) {
        if (player.inventory.getMainHandStack().getItem() == Items.BARRIER) return ActionResult.PASS;
        for (int i = 0; i < counter; i++) {
            BlockPos pos = player.getBlockPos().add(rand.nextGaussian() * dist, rand.nextGaussian() * dist, rand.nextGaussian() * dist);
            if (player.clientWorld.getBlockState(pos).getBlock() != Blocks.BARRIER) continue;
            MinecraftClient.getInstance().particleManager.addParticle(new BarrierParticle.Factory().createParticle(
                    null, player.clientWorld, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 0, 0, 0));
        }
        return ActionResult.PASS;
    }

    private void onCommand(Command cmd, String label, String[] args) {
        try {
            counter = Integer.parseInt(args[0]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
        }
        enabled = !enabled;
        sendMessage(new LiteralText("Barrier display counter is " + counter).formatted(Formatting.GOLD));
    }

    private ActionResult onSave(CompoundTag compoundTag) {
        CompoundTag tag = compoundTag.getCompound("barrier");
        tag.putInt("counter", counter);
        compoundTag.put("barrier", tag);
        return ActionResult.PASS;
    }

    private ActionResult onLoad(CompoundTag compoundTag) {
        CompoundTag tag = compoundTag.getCompound("barrier");
        counter = tag.contains("counter") ? tag.getInt("counter") : 20;
        return ActionResult.PASS;
    }
}
