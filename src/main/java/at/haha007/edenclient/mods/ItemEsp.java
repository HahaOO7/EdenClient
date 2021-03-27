package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.*;
import at.haha007.edenclient.command.Command;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.RenderUtils;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class ItemEsp {
	boolean enabled = false;
	float size = .1f;
	int itemBox;

	void initItemBox() {
		if (itemBox != 0) return;
		itemBox = GL11.glGenLists(1);
		GL11.glNewList(itemBox, GL11.GL_COMPILE);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glColor4f(1, 1, 0, 0.5F);
		RenderUtils.drawOutlinedBox(new Box(-0.5, 0, -0.5, 0.5, 1, 0.5));
		GL11.glEndList();
	}

	void destroy() {
		if (itemBox == 0) return;
		GL11.glDeleteLists(itemBox, 1);
		itemBox = 0;
	}


	public ItemEsp() {
		ItemRenderCallback.EVENT.register(this::renderItem);
		CommandManager.registerCommand(new Command(this::onCommand), "itemesp");
		StartGameSessionCallback.EVENT.register(this::startSession);
		LeaveGameSessionCallback.EVENT.register(this::endSession);
		ConfigSaveCallback.EVENT.register(this::onSave);
		ConfigLoadCallback.EVENT.register(this::onLoad);
	}

	private ActionResult endSession(ClientPlayerEntity clientPlayerEntity) {
		destroy();
		return ActionResult.PASS;
	}

	private ActionResult startSession(ClientPlayerEntity clientPlayerEntity) {
		initItemBox();
		return ActionResult.PASS;
	}

	private ActionResult onLoad(CompoundTag compoundTag) {
		CompoundTag tag = compoundTag.getCompound("itemesp");
		if (!tag.contains("enabled")) return ActionResult.PASS;
		enabled = tag.getBoolean("enabled");
		return ActionResult.PASS;
	}

	private ActionResult onSave(CompoundTag compoundTag) {
		CompoundTag tag = compoundTag.getCompound("itemesp");
		tag.putBoolean("enabled", enabled);
		compoundTag.put("itemesp", tag);
		return ActionResult.PASS;
	}

	private void onCommand(Command cmd, String label, String[] args) {
		enabled = !enabled;
		PlayerUtils.sendMessage(new LiteralText("Item ESP " + (enabled ? "enabled" : "disabled")));
	}


	private ActionResult renderItem(ItemEntity itemEntity, float yaw, float tickDelta, int light) {
		if (!enabled) return ActionResult.PASS;
		
		// GL settings
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2);
		GL11.glDisable(GL11.GL_LIGHTING);

		GL11.glPushMatrix();
		RenderUtils.applyRegionalRenderOffset();

		BlockPos camPos = RenderUtils.getCameraBlockPos();
		int regionX = (camPos.getX() >> 9) * 512;
		int regionZ = (camPos.getZ() >> 9) * 512;

		GL11.glPushMatrix();

		GL11.glTranslated(
			itemEntity.prevX + (itemEntity.getX() - itemEntity.prevX) * tickDelta - regionX,
			itemEntity.prevY + (itemEntity.getY() - itemEntity.prevY) * tickDelta,
			itemEntity.prevZ + (itemEntity.getZ() - itemEntity.prevZ) * tickDelta - regionZ);

		GL11.glPushMatrix();
		GL11.glScaled(itemEntity.getWidth() + size,
			itemEntity.getHeight() + size, itemEntity.getWidth() + size);
		GL30.glCallList(itemBox);
		GL11.glPopMatrix();


		GL11.glPopMatrix();

		GL11.glPopMatrix();

		// GL resets
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);

		return ActionResult.PASS;
	}
}
