package at.haha007.edenclient.mods.CheshShop;

import at.haha007.edenclient.callbacks.PlayerAttackBlockCallback;
import at.haha007.edenclient.command.Command;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.tag.ItemTags;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.Map;

public class ChestShopMod {

	Map<BlockPos, ChestShopEntry> entries = new HashMap<>();


	public ChestShopMod() {
		PlayerAttackBlockCallback.EVENT.register(this::onAttackBlock);
	}

	private ActionResult onAttackBlock(ClientPlayerEntity player, BlockPos blockPos, Direction direction) {
		if (!ItemTags.SIGNS.contains(player.inventory.getMainHandStack().getItem())) return ActionResult.PASS;
		if (!(player.world.getBlockState(blockPos).getBlock() instanceof ChestBlock)) return ActionResult.PASS;

		player.
			world.
			blockEntities.
			stream().
			filter(blockEntity -> blockEntity instanceof SignBlockEntity).
			map(be -> (SignBlockEntity) be).
			map(ChestShopEntry::new).
			filter(ChestShopEntry::isShop).
			forEach(x -> entries.put(x.getPos(), x));

		return ActionResult.CONSUME;
	}

	public void onCommand(Command command, String s, String[] args) {
		StringBuilder sb = new StringBuilder();
		for (String arg : args) {
			sb.append(" ").append(arg);
		}
		String item = sb.toString().replaceFirst(" ", "");
		entries.
			values().
			stream().
			filter(ChestShopEntry::canBuy).
			filter(entry -> entry.getItem().equalsIgnoreCase(item)).
			min((a, b) -> Float.compare(a.getBuyPricePerItem(), b.getBuyPricePerItem())).
			ifPresent(cs ->
				sendMessage(String.format(
					"Buy at %s[%d, %d, %d] for %.2f$/item",
					cs.getOwner(),
					cs.getPos().getX(),
					cs.getPos().getY(),
					cs.getPos().getZ(),
					cs.getBuyPricePerItem())));

		entries.
			values().
			stream().
			filter(ChestShopEntry::canSell).
			filter(entry -> entry.getItem().equalsIgnoreCase(item)).
			max((a, b) -> Float.compare(a.getSellPricePerItem(), b.getSellPricePerItem())).
			ifPresent(cs -> sendMessage(String.format(
				"Sell at %s[%d, %d, %d] for %.2f$/item",
				cs.getOwner(),
				cs.getPos().getX(),
				cs.getPos().getY(),
				cs.getPos().getZ(),
				cs.getSellPricePerItem())));

	}

	private void sendMessage(String message) {
		MinecraftClient client = MinecraftClient.getInstance();
		client.inGameHud.getChatHud().addMessage(new LiteralText(message));
	}


}
