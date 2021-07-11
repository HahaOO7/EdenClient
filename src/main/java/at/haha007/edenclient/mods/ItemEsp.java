package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.ItemRenderCallback;
import at.haha007.edenclient.command.Command;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.RenderUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class ItemEsp {
    boolean enabled = false;
    float size = .1f;
    float r, g, b;
    boolean solid;

    public ItemEsp() {
        ItemRenderCallback.EVENT.register(this::renderItem);
        CommandManager.registerCommand(new Command(this::onCommand), "itemesp");
        ConfigSaveCallback.EVENT.register(this::onSave);
        ConfigLoadCallback.EVENT.register(this::onLoad);
    }

    private ActionResult onLoad(NbtCompound compoundTag) {
        NbtCompound tag = compoundTag.getCompound("itemesp");
        if (tag.contains("enabled")) enabled = tag.getBoolean("enabled");
        if (tag.contains("solid")) enabled = tag.getBoolean("solid");
        if (tag.contains("size")) size = tag.getFloat("size");
        if (tag.contains("r")) r = tag.getFloat("r");
        if (tag.contains("g")) g = tag.getFloat("g");
        if (tag.contains("b")) b = tag.getFloat("b");
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

    private void onCommand(Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sendModMessage(new LiteralText("/itemesp toggle").formatted(Formatting.GOLD));
            sendModMessage(new LiteralText("/itemesp size <size>").formatted(Formatting.GOLD));
            sendModMessage(new LiteralText("/itemesp solid").formatted(Formatting.GOLD));
            sendModMessage(new LiteralText("/itemesp color <r> <g> <b> -> (0-256)").formatted(Formatting.GOLD));
            sendModMessage(new LiteralText("/itemesp line <strength>").formatted(Formatting.GOLD));
            return;
        }
        switch (args[0].toLowerCase()) {
            case "toggle" -> {
                enabled = !enabled;
                sendModMessage(new LiteralText("Item ESP " + (enabled ? "enabled" : "disabled")));
            }
            case "size" -> {
                if (args.length != 2) {
                    sendModMessage(new LiteralText("/itemesp size <size>").formatted(Formatting.GOLD));
                    break;
                }
                try {
                    size = Float.parseFloat(args[1]);
                } catch (NumberFormatException e) {
                    sendModMessage(new LiteralText("Size has to be a number.").formatted(Formatting.GOLD));
                    break;
                }
                sendModMessage(new LiteralText("Size: " + size).formatted(Formatting.GOLD));
            }
            case "color" -> {
                float r, g, b;
                try {
                    r = Float.parseFloat(args[1]) / 256;
                    g = Float.parseFloat(args[2]) / 256;
                    b = Float.parseFloat(args[3]) / 256;
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    sendModMessage(new LiteralText("/itemesp color <r> <g> <b> -> (0-256)").formatted(Formatting.GOLD));
                    return;
                }
                if (r > 1 || b > 1 || g > 1) {
                    sendModMessage(new LiteralText("/itemesp color <r> <g> <b> -> (0-256)").formatted(Formatting.GOLD));
                    return;
                }
                this.r = r;
                this.g = g;
                this.b = b;
            }
            case "solid" -> {
                solid = !solid;
                sendModMessage(new LiteralText("Item ESP " + (solid ? "solid" : "transparent")));
            }
            default -> {
                sendModMessage(new LiteralText("/itemesp toggle").formatted(Formatting.GOLD));
                sendModMessage(new LiteralText("/itemesp size <size>").formatted(Formatting.GOLD));
            }
        }
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
