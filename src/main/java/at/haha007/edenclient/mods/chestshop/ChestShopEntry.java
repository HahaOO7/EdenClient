package at.haha007.edenclient.mods.chestshop;

import at.haha007.edenclient.utils.MathUtils;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3i;

public class ChestShopEntry {
    private Vec3i pos;
    private int sellPrice = -1, buyPrice = -1;
    private int amount;
    private String owner;
    private boolean isShop = false;
    private String item;


    public ChestShopEntry(SignBlockEntity sign) {
        NbtCompound tag = new NbtCompound();
        sign.readNbt(tag);
        String[] lines = new String[4];
        for (int i = 0; i < lines.length; i++) {
            lines[i] = getString(tag.getString("Text" + (i + 1)));
        }

        String player = lines[0];
        if (player.isEmpty()) return;

        int amount;
        try {
            amount = Integer.parseInt(lines[1]);
        } catch (NumberFormatException e) {
            return;
        }

        String item = lines[3];
        if (item.isEmpty()) return;

        String[] prices = lines[2].toLowerCase().replaceAll("\\s", "").split(":");
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
        pos = sign.getPos();
        this.amount = amount;
        this.isShop = canBuy() || canSell();
        this.owner = player;
        this.item = item;
    }

    public ChestShopEntry(NbtCompound tag) {
        isShop = true;
        amount = tag.getInt("amount");
        int[] ints = tag.getIntArray("pos");
        pos = new Vec3i(ints[0], ints[1], ints[2]);
        owner = tag.getString("owner");
        item = tag.getString("item");
        if (tag.contains("buyPrice"))
            buyPrice = tag.getInt("buyPrice");
        if (tag.contains("sellPrice"))
            sellPrice = tag.getInt("sellPrice");
    }

    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
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

    public int getBuyPrice() {
        return buyPrice;
    }

    public float getBuyPricePerItem() {
        return ((float) buyPrice) / amount;
    }

    public float getSellPricePerItem() {
        return ((float) sellPrice) / amount;
    }

    public int getSellPrice() {
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

    private String getString(String string) {
        string = string.
                replaceFirst("\\{", "").
                replaceFirst("\\{", "").
                replaceFirst("\"text\":\"", "").
                replaceFirst("\"text\":\"", "").
                replaceFirst("\"}],", "").
                replaceFirst("\"extra\":\\[", "");
        return string.substring(0, string.length() - 2);
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

}
