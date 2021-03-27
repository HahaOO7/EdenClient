package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.*;
import at.haha007.edenclient.command.Command;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.RenderUtils;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import static at.haha007.edenclient.utils.PlayerUtils.sendMessage;
import static org.lwjgl.opengl.GL11.*;

public class ItemEsp {
	boolean enabled = false;
	float size = .1f;
	int itemBox;

	void initItemBox() {
		if (itemBox != 0) return;
		itemBox = glGenLists(1);
		glNewList(itemBox, GL_COMPILE);
		glEnable(GL_BLEND);
		glDisable(GL_TEXTURE_2D);
		glDisable(GL_DEPTH_TEST);
		glDisable(GL_LIGHTING);
		glColor4f(1, 1, 0, 0.5F);
		RenderUtils.drawOutlinedBox(new Box(-0.5, 0, -0.5, 0.5, 1, 0.5));
		glEndList();
	}

	void destroy() {
		if (itemBox == 0) return;
		glDeleteLists(itemBox, 1);
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
		if (tag.contains("enabled")) enabled = tag.getBoolean("enabled");
		if (tag.contains("size")) size = tag.getFloat("size");
		return ActionResult.PASS;
	}

	private ActionResult onSave(CompoundTag compoundTag) {
		CompoundTag tag = compoundTag.getCompound("itemesp");
		tag.putBoolean("enabled", enabled);
		tag.putFloat("size", size);
		compoundTag.put("itemesp", tag);
		return ActionResult.PASS;
	}

	private void onCommand(Command cmd, String label, String[] args) {
		if (args.length == 0) {
			sendMessage(new LiteralText("/itemesp toggle").formatted(Formatting.GOLD));
			sendMessage(new LiteralText("/itemesp size <size>").formatted(Formatting.GOLD));
			return;
		}
		switch (args[0].toLowerCase()) {
			case "toggle":
				enabled = !enabled;
				sendMessage(new LiteralText("Item ESP " + (enabled ? "enabled" : "disabled")));
				break;

			case "size":
				if (args.length != 2) {
					sendMessage(new LiteralText("/itemesp size <size>").formatted(Formatting.GOLD));
					break;
				}
				try {
					size = Float.parseFloat(args[1]);
				} catch (NumberFormatException e) {
					sendMessage(new LiteralText("Size has to be a number.").formatted(Formatting.GOLD));
					break;
				}
				sendMessage(new LiteralText("Size: " + size).formatted(Formatting.GOLD));
				break;

			default:
				sendMessage(new LiteralText("/itemesp toggle").formatted(Formatting.GOLD));
				sendMessage(new LiteralText("/itemesp size <size>").formatted(Formatting.GOLD));
				break;
		}
	}


	private ActionResult renderItem(ItemEntity itemEntity, float yaw, float tickDelta, int light) {
		if (!enabled) return ActionResult.PASS;

		// GL settings
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(GL_LINE_SMOOTH);
		glLineWidth(2);
		glDisable(GL_LIGHTING);

		glPushMatrix();
		RenderUtils.applyRegionalRenderOffset();

		BlockPos camPos = RenderUtils.getCameraBlockPos();
		int regionX = (camPos.getX() >> 9) * 512;
		int regionZ = (camPos.getZ() >> 9) * 512;

		glPushMatrix();

		glTranslated(
			itemEntity.prevX + (itemEntity.getX() - itemEntity.prevX) * tickDelta - regionX,
			itemEntity.prevY + (itemEntity.getY() - itemEntity.prevY) * tickDelta,
			itemEntity.prevZ + (itemEntity.getZ() - itemEntity.prevZ) * tickDelta - regionZ);

		glPushMatrix();

		glScaled(itemEntity.getWidth() + size,
			itemEntity.getHeight() + size,
			itemEntity.getWidth() + size);
		glCallList(itemBox);

		glPopMatrix();
		glPopMatrix();
		glPopMatrix();

		// GL resets
		glColor4f(1, 1, 1, 1);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_TEXTURE_2D);
		glDisable(GL_BLEND);
		glDisable(GL_LINE_SMOOTH);

		return ActionResult.PASS;
	}
}
