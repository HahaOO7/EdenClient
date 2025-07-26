package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.EdenRenderUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fi.dy.masa.malilib.util.data.Color4f;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class ItemEsp {
    private static final AABB AABB = new AABB(-0.25, -0.0, -0.25, 0.25, 0.5, 0.25);
    @ConfigSubscriber("false")
    boolean enabled = false;
    @ConfigSubscriber("1")
    float red;
    @ConfigSubscriber("1")
    float green;
    @ConfigSubscriber("1")
    float blue;
    List<ItemEntity> items = new ArrayList<>();


    public ItemEsp() {
        registerCommand();
        PerWorldConfig.get().register(this, "itemEsp");
        LeaveWorldCallback.EVENT.register(this::destroy, getClass());
        GameRenderCallback.EVENT.register(this::render, getClass());
        PlayerTickCallback.EVENT.register(this::tick, getClass());
    }

    private void tick(LocalPlayer player) {
        items = player.clientLevel.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(10000, 500, 10000), i -> true);
    }

    private void render(float tickDelta) {
        if (!enabled) return;
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        for (ItemEntity target : items) {
            AABB aabb = AABB.move(target.getPosition(tickDelta));
            EdenRenderUtils.drawAreaOutline(aabb.getMinPosition(), aabb.getMaxPosition(), Color4f.fromColor(new Color(red, green, blue).getRGB()));
        }
    }

    private void destroy() {
        items.clear();
    }

    private void registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> node = literal("eitemesp");

        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendModMessage(("Item ESP " + (enabled ? "enabled" : "disabled")));
            return 1;
        }));

        node.then(literal("color").then(arg("r").then(arg("g").then(arg("b").executes(c -> {
            setColor(c);
            return 0;
        })))));

        node.executes(c -> {
            sendModMessage("/itemesp toggle");
            sendModMessage("/itemesp solid");
            sendModMessage("/itemesp size <size>");
            sendModMessage("/itemesp color <r> <g> <b>");
            return 1;
        });

        register(node,
                "ItemESP allows for all items lying on the ground to be surrounded with their respective x-ray bounding boxes.");
    }

    RequiredArgumentBuilder<FabricClientCommandSource, Integer> arg(String key) {
        return argument(key, IntegerArgumentType.integer(0, 256));
    }

    private void setColor(CommandContext<FabricClientCommandSource> c) {
        this.red = c.getArgument("r", Integer.class) / 256f;
        this.green = c.getArgument("g", Integer.class) / 256f;
        this.blue = c.getArgument("b", Integer.class) / 256f;
        sendModMessage("Color updated.");
    }
}
