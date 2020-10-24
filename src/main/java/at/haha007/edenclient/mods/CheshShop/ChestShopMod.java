package at.haha007.edenclient.mods.CheshShop;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.command.Command;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ChestShopMod {

	private final Map<Vec3i, ChestShopEntry> shops = new HashMap<>();
	private int[] chunk = {0, 0};
	private final File file;
	private String worldName = "";


	public ChestShopMod() {
		PlayerTickCallback.EVENT.register(this::tick);
		file = new File(EdenClient.getDataFolder(), "ChestShop.mca");
	}

	private ActionResult tick(ClientPlayerEntity player) {
		int[] chunk = {player.chunkX, player.chunkZ};
		if (Arrays.equals(this.chunk, chunk)) return ActionResult.PASS;

		String world = StringUtils.getWorldOrServerName();
		if (!worldName.equals(world)) {
			try {
				saveConfig(worldName);
				worldName = world;
				loadConfig(worldName);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.chunk = chunk;
		checkForShops(player);
		try {
			saveConfig(worldName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ActionResult.PASS;
	}

	private void checkForShops(ClientPlayerEntity player) {
		World world = player.world;

		Set<Vec3i> remove = new HashSet<>();

		shops.
			keySet().
			stream().
			filter(this::inDistance).
			forEach(remove::add);

		remove.forEach(shops::remove);

		world.
			blockEntities.
			stream().
			filter(blockEntity -> blockEntity instanceof SignBlockEntity).
			map(be -> (SignBlockEntity) be).
			map(ChestShopEntry::new).
			filter(ChestShopEntry::isShop).
			forEach(x -> shops.put(x.getPos(), x));

	}

	private boolean inDistance(Vec3i pos) {
		return Math.abs(chunk[1] - (pos.getZ() << 4)) <= 2 && Math.abs(chunk[0] - (pos.getX() << 4)) <= 2;
	}

	public void onCommand(Command command, String s, String[] args) {
		if (args.length < 2) {
			sendMessage("/chestshop sell itemtype");
			sendMessage("/chestshop buy itemtype");
			return;
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 1; i < args.length; i++) {
			sb.append(" ").append(args[i]);
		}
		String item = sb.toString().replaceFirst(" ", "");


		switch (args[0].toLowerCase()) {
			case "sell":
				sendMessage("Sell: ");
				shops.
					values().
					stream().
					filter(ChestShopEntry::canSell).
					filter(entry -> entry.getItem().equalsIgnoreCase(item)).
					sorted((a, b) -> Float.compare(a.getSellPricePerItem(), b.getSellPricePerItem())).
					limit(10).
					forEach(cs -> sendMessage(String.format(
						"%s[%d, %d, %d] for %.2f$/item",
						cs.getOwner(),
						cs.getPos().getX(),
						cs.getPos().getY(),
						cs.getPos().getZ(),
						cs.getSellPricePerItem())));
				break;
			case "buy":
				sendMessage("Buy: ");
				shops.
					values().
					stream().
					filter(ChestShopEntry::canSell).
					filter(entry -> entry.getItem().equalsIgnoreCase(item)).
					sorted((b, a) -> Float.compare(a.getSellPricePerItem(), b.getSellPricePerItem())).
					limit(10).
					forEach(cs -> sendMessage(String.format(
						"%s[%d, %d, %d] for %.2f$/item",
						cs.getOwner(),
						cs.getPos().getX(),
						cs.getPos().getY(),
						cs.getPos().getZ(),
						cs.getSellPricePerItem())));
				break;
		}


		shops.
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

	}

	private void saveConfig(String worldName) throws IOException {
		CompoundTag tag = new CompoundTag();
		ListTag list = new ListTag();
		tag.put(worldName, list);

		shops.
			values().
			forEach(cs -> list.add(cs.toTag()));

		if (!file.exists()) if (!file.createNewFile()) throw new IOException();
		NbtIo.writeCompressed(tag, file);
	}

	private void loadConfig(String worldName) throws IOException {
		CompoundTag tag = NbtIo.readCompressed(file);
		ListTag list = tag.getList(worldName, 10);
		list.forEach(nbt -> {
			ChestShopEntry entry = new ChestShopEntry((CompoundTag) nbt);
			if (entry.canSell() || entry.canBuy())
				shops.put(entry.getPos(), entry);
		});
	}

	private void sendMessage(String message) {
		MinecraftClient client = MinecraftClient.getInstance();
		client.inGameHud.getChatHud().addMessage(new LiteralText(message));
	}


}
