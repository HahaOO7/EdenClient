package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

public interface GameRenderCallback {
    Event<GameRenderCallback> EVENT = EventFactory.createArrayBacked(GameRenderCallback.class,
            listeners -> (matrixStack, vertexConsumerProvider, tickDelta) -> {
                for (GameRenderCallback listener : listeners) {
                    listener.render(matrixStack, vertexConsumerProvider, tickDelta);

                }
            });

    void render(MatrixStack matrixStack, VertexConsumerProvider.Immediate vertexConsumerProvider, float tickDelta);
}
