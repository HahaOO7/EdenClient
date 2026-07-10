package at.haha007.edenclient.mixin;

import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FishingHook.class)
public interface FishingHookMixin {
    @Accessor("biting")
    boolean edenclient$isFishBiting();
}
