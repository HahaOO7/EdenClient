package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.EntityRenderCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.RenderUtils;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static at.haha007.edenclient.command.CommandManager.argument;
import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class EntityEsp {
    private boolean enabled;
    private EntityType<?>[] entityTypes;
    boolean solid;
    int r, g, b;

    public EntityEsp() {
        EntityRenderCallback.EVENT.register(this::render);
        ConfigLoadCallback.EVENT.register(this::load);
        ConfigSaveCallback.EVENT.register(this::save);
        registerCommand();
    }

    private void load(NbtCompound nbtCompound) {
        var tag = nbtCompound.getCompound("entityEsp");
        if (tag.isEmpty()) {
            enabled = false;
            solid = false;
            r = g = b = 255;
            entityTypes = new EntityType[0];
            return;
        }
        enabled = tag.getBoolean("enabled");
        enabled = tag.getBoolean("solid");

        r = tag.getInt("r");
        g = tag.getInt("g");
        b = tag.getInt("b");

        NbtList entities = tag.getList("entityTypes", NbtElement.STRING_TYPE);
        DefaultedRegistry<EntityType<?>> registry = Registry.ENTITY_TYPE;
        entityTypes = entities.stream()
                .map(NbtElement::asString)
                .map(Identifier::new)
                .map(registry::get)
                .toList()
                .toArray(new EntityType[0]);

        Arrays.sort(entityTypes, Comparator.comparing(Object::hashCode));

    }

    private void save(NbtCompound nbtCompound) {
        NbtCompound tag = new NbtCompound();
        tag.putBoolean("enabled", enabled);
        tag.putBoolean("solid", solid);

        tag.putInt("r", r);
        tag.putInt("g", g);
        tag.putInt("b", b);

        NbtList entities = new NbtList();
        for (EntityType<?> type : entityTypes) {
            entities.add(NbtString.of(Registry.ENTITY_TYPE.getId(type).toString()));
        }
        tag.put("entityTypes", entities);

        nbtCompound.put("entityEsp", tag);
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientCommandSource> cmd = literal("entityesp");
        LiteralArgumentBuilder<ClientCommandSource> toggle = literal("toggle");
        toggle.executes(c -> {
            enabled = !enabled;
            sendModMessage(enabled ? "EntityEsp enabled" : "EntityEsp disabled");
            return 1;
        });

        DefaultedRegistry<EntityType<?>> registry = Registry.ENTITY_TYPE;
        for (EntityType<?> type : registry) {
            toggle.then(literal(registry.getId(type).toString().replace("minecraft:", "")).executes(c -> {
                if (Arrays.binarySearch(entityTypes, type, Comparator.comparing(Object::hashCode)) < 0) {
                    add(type);
                    sendModMessage("Enabled EntityEsp for EntityType " + type.getUntranslatedName());
                } else {
                    remove(type);
                    sendModMessage("Disabled EntityEsp for EntityType " + type.getUntranslatedName());
                }
                System.out.println(Arrays.toString(entityTypes));
                return 1;
            }));
        }

        cmd.then(literal("solid").executes(c -> {
            solid = !solid;
            sendModMessage(solid ? "EntityEsp solid" : "EntityEsp transparent");
            return 1;
        }));

        cmd.then(literal("color").then(arg("r").then(arg("g").then(arg("b").executes(this::setColor)))));
        cmd.then(toggle);
        CommandManager.register(cmd);
    }

    RequiredArgumentBuilder<ClientCommandSource, Integer> arg(String key) {
        return argument(key, IntegerArgumentType.integer(0, 255));
    }

    private int setColor(CommandContext<ClientCommandSource> c) {
        this.r = c.getArgument("r", Integer.class);
        this.g = c.getArgument("g", Integer.class);
        this.b = c.getArgument("b", Integer.class);
        sendModMessage(new LiteralText("Color updated.").formatted(Formatting.GOLD));
        return 1;
    }

    private void remove(EntityType<?> type) {
        List<EntityType<?>> list = new ArrayList<>(List.of(entityTypes));
        list.remove(type);
        entityTypes = list.toArray(new EntityType[0]);
        Arrays.sort(entityTypes, Comparator.comparing(Object::hashCode));
    }

    private void add(EntityType<?> type) {
        List<EntityType<?>> list = new ArrayList<>(List.of(entityTypes));
        list.add(type);
        entityTypes = list.toArray(new EntityType[0]);
        Arrays.sort(entityTypes, Comparator.comparing(Object::hashCode));
    }

    private void render(Entity entity, float tickDelta, MatrixStack matrixStack) {
        if (!enabled || Arrays.binarySearch(entityTypes, entity.getType(), Comparator.comparing(Object::hashCode)) < 0)
            return;
        matrixStack.push();

        RenderUtils.applyRegionalRenderOffset(matrixStack);
        matrixStack.push();


        BlockPos camPos = RenderUtils.getCameraBlockPos();
        int regionX = (camPos.getX() >> 9) * 512;
        int regionZ = (camPos.getZ() >> 9) * 512;


        matrixStack.translate(
                entity.prevX + (entity.getX() - entity.prevX) * tickDelta - regionX,
                entity.prevY + (entity.getY() - entity.prevY) * tickDelta,
                entity.prevZ + (entity.getZ() - entity.prevZ) * tickDelta - regionZ);

        matrixStack.push();
        matrixStack.scale(2, 2, 2);

        Box box = entity.getType().createSimpleBoundingBox(0, 0, 0);


        if (solid)
            RenderUtils.drawSolidBox(box, matrixStack, r, g, b);
        else
            RenderUtils.drawOutlinedBox(box, matrixStack, r, g, b);

        matrixStack.pop();
        matrixStack.pop();
        matrixStack.pop();
    }
}
