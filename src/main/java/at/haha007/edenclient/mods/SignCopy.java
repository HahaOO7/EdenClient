package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.PlayerAttackBlockCallback;
import at.haha007.edenclient.callbacks.PlayerEditSignCallback;
import at.haha007.edenclient.command.Command;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.tag.ItemTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class SignCopy {
	public static String[] copy = new String[4];
	public static boolean shouldCopy = false;

	public SignCopy() {
		PlayerEditSignCallback.EVENT.register(this::onEditSign);
		PlayerAttackBlockCallback.EVENT.register(this::onAttackBlock);
	}

	private ActionResult onAttackBlock(ClientPlayerEntity entity, BlockPos pos, Direction side) {
		BlockState state = MinecraftClient.getInstance().world.getBlockState(pos);
		ItemStack stack = MinecraftClient.getInstance().player.inventory.getMainHandStack();
		float toolMultiplier = stack.getMiningSpeedMultiplier(state);
		float canHarvest = stack.isEffectiveOn(state) ? 1.5f : 5f;
		System.out.println();

		BlockEntity b = MinecraftClient.getInstance().world.getBlockEntity(pos);
		if (!ItemTags.SIGNS.contains(MinecraftClient.getInstance().player.inventory.getMainHandStack().getItem()))
			return ActionResult.PASS;
		if (!(b instanceof SignBlockEntity)) {
			shouldCopy = false;
			return ActionResult.PASS;
		}
		shouldCopy = true;
		SignBlockEntity sign = (SignBlockEntity) b;
		CompoundTag tag = new CompoundTag();
		tag = sign.toTag(tag);
		copy[0] = getString(tag.getString("Text1"));
		copy[1] = getString(tag.getString("Text2"));
		copy[2] = getString(tag.getString("Text3"));
		copy[3] = getString(tag.getString("Text4"));
		return ActionResult.FAIL;
	}

	private ActionResult onEditSign(ClientPlayerEntity player, SignBlockEntity sign) {
		if (!shouldCopy) return ActionResult.PASS;
		UpdateSignC2SPacket packet = new UpdateSignC2SPacket(sign.getPos(),
			copy[0].substring(0, copy[0].length() - 2),
			copy[1].substring(0, copy[1].length() - 2),
			copy[2].substring(0, copy[2].length() - 2),
			copy[3].substring(0, copy[3].length() - 2));
		MinecraftClient.getInstance().getNetworkHandler().sendPacket(packet);
		return ActionResult.FAIL;
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
