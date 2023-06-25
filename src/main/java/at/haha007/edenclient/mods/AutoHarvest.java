package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

import static at.haha007.edenclient.utils.PlayerUtils.getHitDirectionForBlock;
import static at.haha007.edenclient.utils.PlayerUtils.getPlayer;

public class AutoHarvest {

    private final Set<Block> justReplant = Set.of(Blocks.POTATOES, Blocks.CARROTS, Blocks.NETHER_WART, Blocks.WHEAT, Blocks.BEETROOTS);
    private final Set<Block> sameBelow = Set.of(Blocks.SUGAR_CANE);

    @ConfigSubscriber("false")
    private boolean enabled;
    private int cycle = 0;
    private Level world;
    private BlockPos place = null;
    private boolean farmland = false;

    public AutoHarvest() {
        PerWorldConfig.get().register(this, "autoHarvest");
        PlayerTickCallback.EVENT.register(this::tick);
        LiteralArgumentBuilder<ClientSuggestionProvider> cmd = CommandManager.literal("eautoharvest");
        cmd.executes(c -> {
            enabled = !enabled;
            PlayerUtils.sendModMessage(enabled ? "AutoHarvest enabled" : "AutoHarvest disabled");
            return 1;
        });
        CommandManager.register(cmd);
    }

    private void tick(LocalPlayer player) {
        if (!enabled)
            return;
        if (place != null) {
            clickPos(place);
            place = null;
            return;
        }
        world = player.clientLevel;
        Vec3 pos = player.getEyePosition();
        BlockPos blockPos = player.blockPosition();
        switch (cycle) {
            case 0 ->//harvest
                    BlockPos.withinManhattanStream(blockPos, 5, 5, 5).
                            filter(b -> Vec3.atCenterOf(b).closerThan(pos, 5)).
                            forEach(this::harvestCrop);
            case 1 -> {//select
                boolean found = false;
                if (BlockPos.withinManhattanStream(blockPos, 5, 5, 5).
                        anyMatch(b -> world.getBlockState(b).getBlock() == Blocks.FARMLAND)) {
                    if (selectFarmlandItem()) {
                        found = true;
                    }
                    farmland = true;
                }
                if (!found && BlockPos.withinManhattanStream(blockPos, 5, 5, 5).
                        anyMatch(b -> world.getBlockState(b).getBlock() == Blocks.SOUL_SAND)) {
                    if (selectNetherwartItem()) {
                        found = true;
                    }
                    farmland = false;
                }
                if (!found) {
                    cycle = 0;
                    return;
                }
            }
            case 2 -> {//plant
                Block filter = farmland ? Blocks.FARMLAND : Blocks.SOUL_SAND;
                BlockPos.withinManhattanStream(blockPos, 5, 5, 5).
                        filter(b -> world.getBlockState(b).getBlock() == filter).
                        filter(b -> world.getBlockState(b.above()).getBlock() == Blocks.AIR).
                        forEach(this::clickPos);
            }
        }
        cycle = (cycle + 1) % 3;
    }

    private void harvestCrop(BlockPos pos) {
        //harvest sugarcane
        sameBelow(pos);
        //harvest normal crops
        harvestNormalCrop(pos);
        //harvest nether warts
        harvestNetherWarts(pos);
    }

    private void harvestNormalCrop(BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!justReplant.contains(block)) return;
        if (!(block instanceof CropBlock cropBlock))
            return;
        if (!cropBlock.isMaxAge(state))
            return;
        attackPos(pos);
    }

    private void harvestNetherWarts(BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof NetherWartBlock))
            return;
        if (state.getValue(BlockStateProperties.AGE_3) < 3)
            return;
        attackPos(pos);
    }

    private void sameBelow(BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!sameBelow.contains(block))
            return;
        if (world.getBlockState(pos.below()).getBlock() != block)
            return;
        attackPos(pos);
    }

    private boolean selectNetherwartItem() {
        return selectItem(Items.NETHER_WART);
    }

    private boolean selectFarmlandItem() {
        for (Item item : new Item[]{Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.POTATO, Items.CARROT}) {
            if (selectItem(item))
                return true;
        }
        return false;
    }

    private boolean selectItem(Item item) {
        LocalPlayer player = PlayerUtils.getPlayer();
        Inventory inventory = player.getInventory();
        inventory.setPickedItem(item.getDefaultInstance());
        return inventory.getSelected().getItem() == item;
    }

    private void attackPos(BlockPos target) {
        LocalPlayer player = PlayerUtils.getPlayer();
        ClientPacketListener nh = Minecraft.getInstance().getConnection();
        if (nh == null)
            return;
        nh.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, target, getHitDirectionForBlock(player, target)));
        nh.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, target, getHitDirectionForBlock(player, target)));
    }

    private void clickPos(Vec3i target) {
        BlockPos bp = new BlockPos(target);
        Direction dir = Direction.UP;
        MultiPlayerGameMode im = Minecraft.getInstance().gameMode;
        if (im == null) return;
        im.useItemOn(getPlayer(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(bp.relative(dir)), dir, bp, false));
    }
}
