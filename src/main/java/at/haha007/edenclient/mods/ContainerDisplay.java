package at.haha007.edenclient.mods;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.callbacks.WorldRenderCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.util.math.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.utils.PlayerUtils.getPlayer;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class ContainerDisplay {
    @ConfigSubscriber("false")
    private boolean enabled;
    private Map<BlockPos, List<Item>> entries = new HashMap<>();


    public ContainerDisplay() {
//        GameRenderCallback.EVENT.register(this::renderGame);
        WorldRenderCallback.EVENT.register(this::renderWorld);
        PlayerTickCallback.EVENT.register(this::tick);
        PerWorldConfig.get().register(this, "ContainerDisplay");
        registerCommand();
    }

    private void tick(ClientPlayerEntity player) {
        if (!enabled) {
            return;
        }
        ChunkPos chunkPos = player.getChunkPos();
        entries = new HashMap<>();
        ChunkPos.stream(chunkPos, 2).forEach(cp -> entries.putAll(EdenClient.INSTANCE.getDataFetcher().getContainerInfo().getContainerInfo(cp)));
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientCommandSource> cmd = literal("econtainerdisplay");
        LiteralArgumentBuilder<ClientCommandSource> toggle = literal("toggle");
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
        CommandManager.register(cmd);
    }

    private void renderWorld(MatrixStack matrixStack, VertexConsumerProvider.Immediate vertexConsumerProvider, float v) {
        if (!enabled)
            return;

        //calculate looking direction, rendering offset and rendering angle
        final Direction direction = Direction.getEntityFacingOrder(PlayerUtils.getPlayer())[5];
        final Vec3d offset = Vec3d.of(direction.getVector().add(1, 1, 1)).multiply(.5);
        final Quaternion rotation = direction.getRotationQuaternion();
        if (direction.getAxis() == Direction.Axis.Y) {
            Quaternion horizontal = getPlayer().getHorizontalFacing().getRotationQuaternion();
            horizontal.hamiltonProduct(Direction.NORTH.getRotationQuaternion());
            rotation.hamiltonProduct(horizontal);
        }
        rotation.hamiltonProduct(Direction.NORTH.getRotationQuaternion());

        RenderSystem.enableDepthTest();

        //get the item renderer
        ItemRenderer itemRenderer = MinecraftClient.getInstance().getItemRenderer();

        //rendering
        entries.forEach((pos, items) -> {
            matrixStack.push();

            //move matrix to rendering position
            matrixStack.translate(pos.getX() + offset.getX(), pos.getY() + offset.getY(), pos.getZ() + offset.getZ());
            //rotate to look outwards
            matrixStack.multiply(rotation);
            //scale item down

            int loopCount = Math.min(items.size(), 9);

            if (loopCount > 1) {
                //multiple items -> render 3x3 items
                matrixStack.scale(.3f, .3f, .3f);

                //loop over items, max 9 times
                for (int i = 0; i < loopCount; i++) {
                    Item item = items.get(i);
                    int x = i / 3;
                    int y = i % 3;
                    matrixStack.push();
                    matrixStack.translate(1 - y, 1 - x, 0);
                    matrixStack.scale(.8f, .8f, .8f);
                    itemRenderer.renderItem(
                            item.getDefaultStack(),
                            ModelTransformation.Mode.NONE,
                            false,
                            matrixStack,
                            vertexConsumerProvider,
                            255,
                            OverlayTexture.DEFAULT_UV,
                            itemRenderer.getModels().getModel(item));
                    matrixStack.pop();
                }
            } else {
                //one item -> render it BIG!
                matrixStack.scale(.6f, .6f, .6f);
                Item item = items.get(0);

                itemRenderer.renderItem(
                        item.getDefaultStack(),
                        ModelTransformation.Mode.NONE,
                        false,
                        matrixStack,
                        vertexConsumerProvider,
                        255,
                        OverlayTexture.DEFAULT_UV,
                        itemRenderer.getModels().getModel(item));
            }
            matrixStack.pop();
        });
    }
}
