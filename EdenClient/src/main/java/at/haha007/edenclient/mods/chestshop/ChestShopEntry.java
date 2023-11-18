package at.haha007.edenclient.mods.chestshop;

import at.haha007.edenclient.utils.Utils;
import lombok.Data;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.util.Objects;

@Data
public class ChestShopEntry {
    private Vec3i pos;
    private Vec3i chestPos;
    private int fullSellPrice = -1;
    private int fullBuyPrice = -1;
    private int amount;
    private String owner;
    private boolean isShop = false;
    private String item;
    private int stock = -1;
    private ChestType chestType;

    public ChestShopEntry(SignBlockEntity sign) {
        String[] linesFront = new String[4];
        for (int i = 0; i < linesFront.length; i++) {
            linesFront[i] = sign.getFrontText().getMessage(i, true).getString().trim();
        }

        String player = linesFront[0];
        if (player.isEmpty()) return;

        if (!Utils.isInteger(linesFront[1])) return;
        int amount = Integer.parseInt(linesFront[1]);

        String item = linesFront[3];
        if (item.isEmpty()) return;

        String[] prices = linesFront[2].toLowerCase().replaceAll("\\s", "").split(":");
        for (String priceString : prices) {
            if (priceString.contains("b")) {
                priceString = priceString.replace("b", "");
                if (!Utils.isInteger(priceString)) continue;
                this.fullBuyPrice = Integer.parseInt(priceString);
            } else if (priceString.contains("s")) {
                priceString = priceString.replace("s", "");
                if (!Utils.isInteger(priceString)) continue;
                this.fullSellPrice = Integer.parseInt(priceString);
            } else {
                return;
            }
        }
        pos = sign.getBlockPos();
        this.amount = amount;
        this.isShop = canBuy() || canSell();
        this.owner = player;
        this.item = item.toLowerCase();
    }

    public ChestShopEntry(CompoundTag tag) {
        isShop = true;
        amount = tag.getInt("amount");
        int[] ints = tag.getIntArray("pos");
        pos = new Vec3i(ints[0], ints[1], ints[2]);
        int[] ints2 = tag.getIntArray("chestpos");
        chestPos = new Vec3i(ints2[0], ints2[1], ints2[2]);
        owner = tag.getString("owner");
        item = tag.getString("item").toLowerCase();
        if (tag.contains("buyPrice"))
            fullBuyPrice = tag.getInt("buyPrice");
        if (tag.contains("sellPrice"))
            fullSellPrice = tag.getInt("sellPrice");
        stock = tag.getInt("stock");
        int chestTypeInt = tag.getInt("chestType");
        chestType = ChestType.values()[chestTypeInt];
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("pos", new int[]{pos.getX(), pos.getY(), pos.getZ()});
        tag.putIntArray("chestpos", new int[]{chestPos.getX(), chestPos.getY(), chestPos.getZ()});
        tag.putString("owner", owner);
        tag.putString("item", item);
        tag.putInt("amount", amount);
        if (canBuy())
            tag.putInt("buyPrice", fullBuyPrice);
        if (canSell())
            tag.putInt("sellPrice", fullSellPrice);
        tag.putInt("stock", stock);
        tag.putInt("chestType", this.chestType.ordinal());
        return tag;
    }

    public boolean canSell() {
        return fullSellPrice >= 0;
    }

    public boolean canBuy() {
        return fullBuyPrice >= 0;
    }

    public float getBuyPricePerItem() {
        return ((float) fullBuyPrice) / amount;
    }

    public float getSellPricePerItem() {
        return ((float) fullSellPrice) / amount;
    }

    public ChunkPos getChunkPos() {
        return new ChunkPos(new BlockPos(pos));
    }

    public int getMaxStock() {
        int slots = chestType == ChestType.SINGLE ? 27 : 54;
        return slots * 64;
    }

    public String toString() {
        if (!isShop) return String.format("[%s] Not a Shop", pos.toString());
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("pos:[%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ()));
        sb.append(String.format(", owner:%s", owner));
        sb.append(String.format(", item:%s", item));
        sb.append(String.format(", amount:%d", amount));
        if (canBuy()) {
            sb.append(String.format(", buy:%d$, bpi:%3f1$/i", fullBuyPrice, getBuyPricePerItem()));
        }
        if (canSell()) {
            sb.append(String.format(", sell:%d$, spi:%3f1$/i", fullSellPrice, getSellPricePerItem()));
        }
        sb.append(String.format(", stock:%d", stock));
        return sb.toString();
    }

    public String formattedString(boolean buy) {
        return String.format(
                "%s [%d, %d, %d] for %.2f$/item",
                getOwner(),
                getPos().getX(),
                getPos().getY(),
                getPos().getZ(),
                buy ? getBuyPricePerItem() : getSellPricePerItem());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChestShopEntry entry = (ChestShopEntry) o;
        return fullSellPrice == entry.fullSellPrice && fullBuyPrice == entry.fullBuyPrice
                && amount == entry.amount && isShop == entry.isShop
                && Objects.equals(pos, entry.pos) && Objects.equals(owner, entry.owner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos, fullSellPrice, fullBuyPrice, amount, owner, isShop, item);
    }
}
