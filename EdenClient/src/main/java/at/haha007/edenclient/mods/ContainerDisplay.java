package at.haha007.edenclient.mods;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.mods.datafetcher.ContainerInfo;
import at.haha007.edenclient.mods.datafetcher.DataFetcher;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.utils.PlayerUtils.getPlayer;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod(dependencies = DataFetcher.class)
public class ContainerDisplay {
    @ConfigSubscriber("false")
    private boolean enabled;
    private Map<Vec3i, ContainerInfo.ChestInfo> entries = new ConcurrentHashMap<>();


    public ContainerDisplay() {
        GameRenderCallback.EVENT.register(this::renderWorld, getClass());
        PlayerTickCallback.EVENT.register(this::tick, getClass());
        PerWorldConfig.get().register(this, "ContainerDisplay");
        registerCommand();
    }

    private void tick(LocalPlayer player) {
        if (!enabled) {
            return;
        }
        ChunkPos chunkPos = player.chunkPosition();
        entries = new ConcurrentHashMap<>();
        ChunkPos.rangeClosed(chunkPos, 1).forEach(cp -> entries.putAll(EdenClient.getMod(DataFetcher.class).getContainerInfo().getContainerInfo(cp)));
        Vec3i pp = player.blockPosition();
        entries = entries.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getKey().distManhattan(pp)))
                .limit(200)
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> cmd = literal("econtainerdisplay");
        LiteralArgumentBuilder<FabricClientCommandSource> toggle = literal("toggle");
        toggle.executes(c -> {
            enabled = !enabled;
            sendModMessage(enabled ? "TileEntityEsp enabled" : "TileEntityEsp disabled");
            return 1;
        });

        cmd.then(literal("clear").executes(c -> {
            entries.clear();
            sendModMessage("Cleared cached containers.");
            return 1;
        }));

        cmd.then(toggle);
        CommandManager.register(cmd, "Displays icons on top of containers.");
    }


    private void renderWorld(PoseStack matrixStack, MultiBufferSource.BufferSource vertexConsumerProvider, float v) {
        if (!enabled)
            return;
        Player player = getPlayer();
        Level level = player.level();

        RenderSystem.enableDepthTest();
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        entries.forEach((pos, chestInfo) -> {
            matrixStack.pushPose();
            //calculate looking direction, rendering offset and rendering angle
            final Direction direction = chestInfo.face();
            final Vec3 offset = Vec3.atLowerCornerOf(direction.getUnitVec3i().offset(1, 1, 1)).scale(.5);
            final Quaternionf rotation = direction.getRotation();
            if (direction.getAxis() == Direction.Axis.Y) {
                Quaternionf horizontal = getPlayer().getDirection().getRotation();
                horizontal.mul(Direction.NORTH.getRotation());
                rotation.mul(horizontal);
                rotation.mul(Direction.NORTH.getRotation());
            } else {
                rotation.mul(Direction.EAST.getRotation());
                rotation.rotateZ(-Mth.HALF_PI);
            }

            //move matrix to rendering position
            matrixStack.translate(pos.getX() + offset.x(), pos.getY() + offset.y(), pos.getZ() + offset.z());
            //rotate to look outwards
            matrixStack.mulPose(rotation);
            //scale item down
            List<Item> items = chestInfo.items();
            int loopCount = Math.min(items.size(), 9);
            if (loopCount > 1) {
                //multiple items -> render 3x3 items
                matrixStack.scale(.3f, .3f, .3f);

                //loop over items, max 9 times
                for (int i = 0; i < loopCount; i++) {
                    Item item = items.get(i);
                    int x = 1 - i / 3;
                    int y = 1 - i % 3;
                    matrixStack.pushPose();
                    matrixStack.translate(y, x, 0);
                    matrixStack.scale(.8f, .8f, .8f);
                    itemRenderer.renderStatic(
                            item.getDefaultInstance(),
                            ItemDisplayContext.FIXED,
                            0xF000F0,
                            OverlayTexture.NO_OVERLAY,
                            matrixStack,
                            vertexConsumerProvider,
                            level,
                            0);
                    matrixStack.popPose();
                }
            } else {
                //one item -> render it BIG!
                matrixStack.scale(.6f, .6f, .6f);
                Item item = items.getFirst();

                itemRenderer.renderStatic(
                        item.getDefaultInstance(),
                        ItemDisplayContext.FIXED,
                        0xF000F0,
                        OverlayTexture.NO_OVERLAY,
                        matrixStack,
                        vertexConsumerProvider,
                        level,
                        0);
            }
            matrixStack.popPose();
        });
        vertexConsumerProvider.endBatch();
    }
}
