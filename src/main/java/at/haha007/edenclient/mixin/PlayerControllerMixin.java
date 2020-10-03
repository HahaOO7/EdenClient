package at.haha007.edenclient.mixin;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tag.ItemTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static at.haha007.edenclient.EdenClient.copy;
import static at.haha007.edenclient.EdenClient.shouldCopy;

@Mixin(ClientPlayerInteractionManager.class)
public class PlayerControllerMixin {

	@Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
	private void onAttackBlock(BlockPos pos, Direction side, CallbackInfoReturnable<Boolean> cir) {
		BlockEntity b = MinecraftClient.getInstance().world.getBlockEntity(pos);
		if (!ItemTags.SIGNS.contains(MinecraftClient.getInstance().player.inventory.getMainHandStack().getItem())) return;
		if (!(b instanceof SignBlockEntity)) {
			shouldCopy = false;
			return;
		}
		shouldCopy = true;
		SignBlockEntity sign = (SignBlockEntity) b;
		CompoundTag tag = new CompoundTag();
		tag = sign.toTag(tag);
		copy[0] = getString(tag.getString("Text1"));
		copy[1] = getString(tag.getString("Text2"));
		copy[2] = getString(tag.getString("Text3"));
		copy[3] = getString(tag.getString("Text4"));
		cir.cancel();
	}

	private String getString(String string) {
		return string.
			replaceFirst("\\{", "").
			replaceFirst("\\{", "").
			replaceFirst("\"text\":\"", "").
			replaceFirst("\"text\":\"", "").
			replaceFirst("\"}],", "").
			replaceFirst("\"extra\":\\[", "");
	}
}
