package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.ItemRenderCallback;
import at.haha007.edenclient.utils.RenderUtils;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class ItemEsp {
    boolean enabled = false;
    float size = .1f;
    float r, g, b;
    boolean solid;

    public ItemEsp() {
        ItemRenderCallback.EVENT.register(this::renderItem);
        registerCommand();
        ConfigSaveCallback.EVENT.register(this::onSave);
        ConfigLoadCallback.EVENT.register(this::onLoad);
    }

    private ActionResult onLoad(NbtCompound compoundTag) {
        NbtCompound tag = compoundTag.getCompound("itemesp");
        enabled = tag.getBoolean("enabled");
        solid = tag.getBoolean("solid");
        if (tag.contains("size")) size = tag.getFloat("size");
        else size = 0.3f;
        if (tag.contains("r")) r = tag.getFloat("r");
        else r = 1;
        if (tag.contains("g")) g = tag.getFloat("g");
        else g = 1;
        if (tag.contains("b")) b = tag.getFloat("b");
        else b = 1;
        return ActionResult.PASS;
    }

    private ActionResult onSave(NbtCompound compoundTag) {
        NbtCompound tag = new NbtCompound();
        tag.putBoolean("enabled", enabled);
        tag.putBoolean("solid", solid);
        tag.putFloat("size", size);
        tag.putFloat("r", r);
        tag.putFloat("g", g);
        tag.putFloat("b", b);
        compoundTag.put("itemesp", tag);
        return ActionResult.PASS;
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientCommandSource> node = literal("itemesp");
        node.then(literal("toggle").executes(c -> {
            sendModMessage(new LiteralText("Item ESP " + ((enabled = !enabled) ? "enabled" : "disabled")));
            return 1;
        }));
        node.then(literal("solid").executes(c -> {
            sendModMessage(new LiteralText("Item ESP " + ((solid = !solid) ? "solid" : "transparent")));
            return 1;
        }));
        node.then(literal("size").then(argument("size", FloatArgumentType.floatArg(0.1f)).executes(c -> {
            size = c.getArgument("size", Float.class);
            sendModMessage(new LiteralText("Size: " + size).formatted(Formatting.GOLD));
            return 1;
        })));
        node.then(literal("color").then(arg("r").then(arg("g")).then(arg("b")).executes(c -> {
            setColor(c);
            return 0;
        })));
        node.executes(c -> {
            sendModMessage(new LiteralText("/itemesp toggle"));
            sendModMessage(new LiteralText("/itemesp solid"));
            sendModMessage(new LiteralText("/itemesp size <size>"));
            sendModMessage(new LiteralText("/itemesp color <r> <g> <b>"));
            return 1;
        });
        register(node);
    }

    RequiredArgumentBuilder<ClientCommandSource, Integer> arg(String key) {
        return argument(key, IntegerArgumentType.integer(0, 255));
    }

    private void setColor(CommandContext<ClientCommandSource> c) {
        this.r =  c.getArgument("r", Integer.class);
        this.g =  c.getArgument("g", Integer.class);
        this.b =  c.getArgument("b", Integer.class);
        sendModMessage(new LiteralText("Color updated.").formatted(Formatting.GOLD));
    }

    private ActionResult renderItem(ItemEntity itemEntity, float yaw, float tickDelta, int light, MatrixStack matrixStack) {
        if (!enabled) return ActionResult.PASS;

        matrixStack.push();

        RenderUtils.applyRegionalRenderOffset(matrixStack);
        matrixStack.push();

        BlockPos camPos = RenderUtils.getCameraBlockPos();
        int regionX = (camPos.getX() >> 9) * 512;
        int regionZ = (camPos.getZ() >> 9) * 512;

        matrixStack.translate(
                itemEntity.prevX + (itemEntity.getX() - itemEntity.prevX) * tickDelta - regionX,
                itemEntity.prevY + (itemEntity.getY() - itemEntity.prevY) * tickDelta,
                itemEntity.prevZ + (itemEntity.getZ() - itemEntity.prevZ) * tickDelta - regionZ);

        matrixStack.push();
        matrixStack.scale(size, size, size);

        if (solid)
            RenderUtils.drawSolidBox(new Box(-0.5, 0, -0.5, 0.5, 1, 0.5), matrixStack, r, g, b);
        else
            RenderUtils.drawOutlinedBox(new Box(-0.5, 0, -0.5, 0.5, 1, 0.5), matrixStack, r, g, b);

        matrixStack.pop();
        matrixStack.pop();
        matrixStack.pop();
        return ActionResult.PASS;
    }
}
