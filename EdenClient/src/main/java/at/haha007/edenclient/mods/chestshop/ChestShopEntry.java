package at.haha007.edenclient.mods.chestshop;

import at.haha007.edenclient.utils.MathUtils;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;

public class ChestShopEntry {
    @Getter
    private Vec3i pos;
    private int sellPrice = -1;
    private int buyPrice = -1;
    private int amount;
    @Getter
    private String owner;
    private boolean isShop = false;
    @Getter
    private String item;

    public ChestShopEntry(SignBlockEntity sign) {
        String[] linesFront = new String[4];
        for (int i = 0; i < linesFront.length; i++) {
            linesFront[i] = sign.getFrontText().getMessage(i, true).getString().trim();
        }

        String player = linesFront[0];
        if (player.isEmpty()) return;

        if (!MathUtils.isInteger(linesFront[1])) return;
        int signAmount = Integer.parseInt(linesFront[1]);

        String signItem = linesFront[3];
        if (signItem.isEmpty()) return;

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
        this.amount = signAmount;
        this.isShop = canBuy() || canSell();
        this.owner = player;
        this.item = signItem.toLowerCase();
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

    public float getSellPricePerItem() {
        return ((float) sellPrice) / amount;
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
}
