package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;

public interface EntityRenderCallback {
    Event<EntityRenderCallback> EVENT = EventFactory.createArrayBacked(EntityRenderCallback.class,
            listeners -> (entity, tickDelta, matrixStack) -> {
                for (EntityRenderCallback listener : listeners) {
                    listener.onRender(entity, tickDelta, matrixStack);
                }
            });

    void onRender(Entity entity, float tickDelta, MatrixStack matrixStack);
}
