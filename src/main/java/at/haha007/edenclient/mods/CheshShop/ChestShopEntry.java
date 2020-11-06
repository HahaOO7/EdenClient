package at.haha007.edenclient.mods.CheshShop;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.ItemEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.registry.Registry;

import java.util.Optional;
import java.util.stream.StreamSupport;

public class ChestShopEntry {

	private static final JsonParser jsonParser = new JsonParser();

	private final Vec3i pos;
	private float price = -1;
	private int amount;
	private String owner;
	private boolean isShop = false;
	private boolean buy;
	private String item;


	public ChestShopEntry(SignBlockEntity sign) {
		CompoundTag tag = new CompoundTag();
		sign.toTag(tag);
		JsonObject[] lines = new JsonObject[4];
		for (int i = 0; i < lines.length; i++) {
			lines[i] = jsonParser.parse(tag.getString("Text" + (i + 1))).getAsJsonObject();
		}
		pos = sign.getPos();


		try {
			//owner
			owner = lines[3].
				getAsJsonArray("extra").
				get(0).
				getAsJsonObject().
				get("text").
				getAsString();
			if (owner.isEmpty()) return;


			//sell/buy
			JsonArray titleArr = lines[0].getAsJsonArray("extra");
			JsonObject title = titleArr.get(1).getAsJsonObject();
			if (!title.get("bold").getAsBoolean() &&
				!title.get("italic").getAsBoolean() &&
				title.get("color").getAsString().equals("gold")) {
				switch (title.get("text").getAsString()) {
					case "Ankauf ":
						buy = false;
						break;
					case "Verkauf ":
						buy = true;
						break;
					default:
						isShop = false;
						return;
				}
			} else return;

			//price
			String priceString = lines[2].getAsJsonArray("extra").get(0).getAsJsonObject().get("text").getAsString().replace(" $", "");
			price = Float.parseFloat(priceString.replace("k", ""));
			if (priceString.contains("k")) price *= 1000;
			if (price <= 0) throw new NumberFormatException();


			//amount
			amount = lines[1].getAsJsonArray("extra").get(1).getAsJsonObject().get("text").getAsInt();
			if (amount <= 0) throw new NumberFormatException();

			//itemtype
			Direction facing = sign.getCachedState().get(WallSignBlock.FACING);
			Vec3d itemPos = Vec3d.ofBottomCenter(sign.getPos().subtract(facing.getVector()).up());
			ClientWorld world = MinecraftClient.getInstance().world;
			if (world == null) return;
			Optional<ItemEntity> optionalItem = StreamSupport.stream(world.getEntities().spliterator(), true).
				filter(a -> a instanceof ItemEntity).filter(a -> a.squaredDistanceTo(itemPos) < 0.5).findFirst().map(a -> (ItemEntity) a);
			if (!optionalItem.isPresent()) return;


			item = Registry.ITEM.getId(optionalItem.get().getStack().getItem()).toString();


			isShop = true;
		} catch (IndexOutOfBoundsException | NullPointerException | IllegalArgumentException ignored) {
		}
	}

	public ChestShopEntry(CompoundTag tag) {
		isShop = true;
		amount = tag.getInt("amount");
		int[] ints = tag.getIntArray("pos");
		pos = new Vec3i(ints[0], ints[1], ints[2]);
		owner = tag.getString("owner");
		item = tag.getString("item");
		price = tag.getInt("price");
		buy = tag.getBoolean("buy");
	}

	public CompoundTag toTag() {
		CompoundTag tag = new CompoundTag();
		tag.putIntArray("pos", new int[]{pos.getX(), pos.getY(), pos.getZ()});
		tag.putString("owner", owner);
		tag.putString("item", item);
		tag.putInt("amount", amount);
		tag.putFloat("price", price);
		tag.putBoolean("buy", buy);
		return tag;
	}

	public boolean isShop() {
		return isShop;
	}

	public boolean canSell() {
		return isShop && !buy;
	}

	public boolean canBuy() {
		return isShop && buy;
	}

	public float getBuyPrice() {
		return canBuy() ? price : -1;
	}

	public float getBuyPricePerItem() {
		return price / amount;
	}

	public float getSellPricePerItem() {
		return canSell() ? price / amount : -1;
	}

	public float getSellPrice() {
		return canSell() ? price : -1;
	}

	public String getItem() {
		return item;
	}

	public String getOwner() {
		return owner;
	}

	public Vec3i getPos() {
		return pos;
	}

	public String toString() {
		if (!isShop) return String.format("[%s] Not a Shop", pos.toString());
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("pos:[%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ()));
		sb.append(String.format(", owner:%s", owner));
		sb.append(String.format(", item:%s", item));
		sb.append(String.format(", amount:%d", amount));
		sb.append(String.format(", %s:%.2f$, bpi:%3f1$/i", canBuy() ? "buy" : "sell", price, getBuyPricePerItem()));
		return sb.toString();
	}

}
