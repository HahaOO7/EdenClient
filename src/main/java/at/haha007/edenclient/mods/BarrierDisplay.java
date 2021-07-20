package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.particle.ItemBillboardParticle;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class BarrierDisplay {
    private static final int dist = 5;
    private final Random rand = new Random();
    private int counter = 20;

    public BarrierDisplay() {
        registerCommand();
        PlayerTickCallback.EVENT.register(this::onTick);
        ConfigSaveCallback.EVENT.register(this::onSave);
        ConfigLoadCallback.EVENT.register(this::onLoad);
    }

    private ActionResult onTick(ClientPlayerEntity player) {
        if (player.getInventory().getMainHandStack().getItem() == Items.BARRIER) return ActionResult.PASS;
        for (int i = 0; i < counter; i++) {
            BlockPos pos = player.getBlockPos().add(rand.nextGaussian() * dist, rand.nextGaussian() * dist, rand.nextGaussian() * dist);
            if (player.clientWorld.getBlockState(pos).getBlock() != Blocks.BARRIER) continue;
            MinecraftClient.getInstance().particleManager.addParticle(new ItemBillboardParticle.BarrierFactory().createParticle(
                    null, player.clientWorld, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 0, 0, 0));
        }
        return ActionResult.PASS;
    }

    private void registerCommand() {
        register(literal("barrier").then(argument("count", IntegerArgumentType.integer(0, 10000)).executes(c -> {
            counter = c.getArgument("count", Integer.class);
            sendModMessage(new LiteralText("Barrier display counter is " + counter).formatted(Formatting.GOLD));
            return 1;
        })).executes(c -> {
            sendModMessage(new LiteralText("/barrier <count>"));
            return 1;
        }));
    }

    private ActionResult onSave(NbtCompound compoundTag) {
        NbtCompound tag = compoundTag.getCompound("barrier");
        tag.putInt("counter", counter);
        compoundTag.put("barrier", tag);
        return ActionResult.PASS;
    }

    private ActionResult onLoad(NbtCompound compoundTag) {
        NbtCompound tag = compoundTag.getCompound("barrier");
        counter = tag.contains("counter") ? tag.getInt("counter") : 20;
        return ActionResult.PASS;
    }
}
