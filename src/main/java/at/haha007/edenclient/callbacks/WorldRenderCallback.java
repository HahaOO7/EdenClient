package at.haha007.edenclient.callbacks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;

public interface WorldRenderCallback {
    Event<GameRenderCallback> EVENT = EventFactory.createArrayBacked(GameRenderCallback.class,
            listeners -> (matrixStack, vertexConsumerProvider, tickDelta) -> {
                for (GameRenderCallback listener : listeners) {
                    listener.render(matrixStack, vertexConsumerProvider, tickDelta);

                }
            });

    void render(PoseStack matrixStack, Camera camera, MultiBufferSource.BufferSource vertexConsumerProvider, float tickDelta);
}
