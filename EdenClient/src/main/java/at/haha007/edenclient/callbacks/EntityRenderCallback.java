package at.haha007.edenclient.callbacks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.Entity;

public interface EntityRenderCallback {
    Event<EntityRenderCallback> EVENT = EventFactory.createArrayBacked(EntityRenderCallback.class,
            listeners -> (entity, tickDelta, matrixStack) -> {
                for (EntityRenderCallback listener : listeners) {
                    listener.onRender(entity, tickDelta, matrixStack);
                }
            });

    void onRender(Entity entity, float tickDelta, PoseStack matrixStack);
}
