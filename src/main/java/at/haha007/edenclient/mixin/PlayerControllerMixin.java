package at.haha007.edenclient.mixin;

import at.haha007.edenclient.EdenClient;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tag.ItemTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
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

	@Inject(at = @At("HEAD"),
		method = "interactBlock",
		cancellable = true)
	void interactBlock(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> ci) {
		if (EdenClient.INSTANCE.autoMiner.interactBlock(world, hitResult)) ci.setReturnValue(ActionResult.FAIL);
	}

	@Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
	private void onAttackBlock(BlockPos pos, Direction side, CallbackInfoReturnable<Boolean> cir) {
		if (EdenClient.INSTANCE.autoMiner.attackBlock(pos, side)) cir.setReturnValue(false);
		BlockState state = MinecraftClient.getInstance().world.getBlockState(pos);
		ItemStack stack = MinecraftClient.getInstance().player.inventory.getMainHandStack();
		float toolMultiplier = stack.getMiningSpeedMultiplier(state);
		float canHarvest = stack.isEffectiveOn(state) ? 1.5f : 5f;
		System.out.println();

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
