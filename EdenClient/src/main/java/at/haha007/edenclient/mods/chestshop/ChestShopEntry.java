package at.haha007.edenclient.mods.chestshop;

import at.haha007.edenclient.utils.MathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;

import java.util.Objects;

public class ChestShopEntry {
    private Vec3i pos;
    private int sellPrice = -1, buyPrice = -1;

    private int amount;
    private String owner;
    private boolean isShop = false;
    private String item;
    private int stock = -1;

    public ChestShopEntry(SignBlockEntity sign) {
        String[] linesFront = new String[4];
        for (int i = 0; i < linesFront.length; i++) {
            linesFront[i] = sign.getFrontText().getMessage(i, true).getString().trim();
        }

        String player = linesFront[0];
        if (player.isEmpty()) return;

        if (!MathUtils.isInteger(linesFront[1])) return;
        int amount = Integer.parseInt(linesFront[1]);

        String item = linesFront[3];
        if (item.isEmpty()) return;

        String[] prices = linesFront[2].toLowerCase().replaceAll("\\s", "").split(":");
        for (String priceString : prices) {
            if (priceString.contains("b")) {
                priceString = priceString.replace("b", "");
                if (!MathUtils.isInteger(priceString)) continue;
                this.buyPrice = Integer.parseInt(priceString);
            } else if (priceString.contains("s")) {
                priceString = priceString.replace("s", "");
                if (!MathUtils.isInteger(priceString)) continue;
                this.sellPrice = Integer.parseInt(priceString);
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
        owner = tag.getString("owner");
        item = tag.getString("item").toLowerCase();
        if (tag.contains("buyPrice"))
            buyPrice = tag.getInt("buyPrice");
        if (tag.contains("sellPrice"))
            sellPrice = tag.getInt("sellPrice");
        stock = tag.getInt("stock");
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("pos", new int[]{pos.getX(), pos.getY(), pos.getZ()});
        tag.putString("owner", owner);
        tag.putString("item", item);
        tag.putInt("amount", amount);
        if (canBuy())
            tag.putInt("buyPrice", buyPrice);
        if (canSell())
            tag.putInt("sellPrice", sellPrice);
        tag.putInt("stock", stock);
        return tag;
    }

    public boolean isShop() {
        return isShop;
    }

    public boolean canSell() {
        return sellPrice >= 0;
    }

    public boolean canBuy() {
        return buyPrice >= 0;
    }

    public float getBuyPricePerItem() {
        return ((float) buyPrice) / amount;
    }

    public int getFullBuyPrice() {
        return buyPrice;
    }

    public float getSellPricePerItem() {
        return ((float) sellPrice) / amount;
    }

    public int getFullSellPrice() {
        return sellPrice;
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

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        System.out.println("STOCK SETTER");
        this.stock = stock;
    }

    public int getAmount() {
        return amount;
    }

    public String toString() {
        if (!isShop) return String.format("[%s] Not a Shop", pos.toString());
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("pos:[%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ()));
        sb.append(String.format(", owner:%s", owner));
        sb.append(String.format(", item:%s", item));
        sb.append(String.format(", amount:%d", amount));
        if (canBuy()) {
            sb.append(String.format(", buy:%d$, bpi:%3f1$/i", buyPrice, getBuyPricePerItem()));
        }
        if (canSell()) {
            sb.append(String.format(", sell:%d$, spi:%3f1$/i", sellPrice, getSellPricePerItem()));
        }
        sb.append(String.format(", stock:%d", stock));
        return sb.toString();
    }

    public ChunkPos getChunkPos() {
        return new ChunkPos(new BlockPos(pos));
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
        return sellPrice == entry.sellPrice && buyPrice == entry.buyPrice
                && amount == entry.amount && isShop == entry.isShop
                && Objects.equals(pos, entry.pos) && Objects.equals(owner, entry.owner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos, sellPrice, buyPrice, amount, owner, isShop, item);
    }
}
