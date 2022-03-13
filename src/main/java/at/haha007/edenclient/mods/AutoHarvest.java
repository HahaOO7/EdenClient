package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.Set;

import static at.haha007.edenclient.utils.PlayerUtils.getHitDirectionForBlock;

public class AutoHarvest {

    private final Set<Block> justReplant = Set.of(Blocks.POTATOES, Blocks.CARROTS, Blocks.NETHER_WART, Blocks.WHEAT, Blocks.BEETROOTS);
    private final Set<Block> sameBelow = Set.of(Blocks.SUGAR_CANE);

    @ConfigSubscriber("false")
    private boolean enabled;
    private int cycle = 0;
    private World world;
    private BlockPos place = null;
    private boolean farmland = false;

    public AutoHarvest() {
        PerWorldConfig.get().register(this, "autoHarvest");
        PlayerTickCallback.EVENT.register(this::tick);
        LiteralArgumentBuilder<ClientCommandSource> cmd = CommandManager.literal("eautoharvest");
        cmd.executes(c -> {
            enabled = !enabled;
            PlayerUtils.sendModMessage(enabled ? "AutoHarvest enabled" : "AutoHarvest disabled");
            return 1;
        });
        CommandManager.register(cmd);
    }

    private void tick(ClientPlayerEntity player) {
        if (!enabled)
            return;
        if (place != null) {
            clickPos(place);
            place = null;
            return;
        }
        world = player.clientWorld;
        Vec3d pos = player.getEyePos();
        BlockPos blockPos = player.getBlockPos();
        switch (cycle) {
            case 0 ->//harvest
                    BlockPos.streamOutwards(blockPos, 5, 5, 5).
                            filter(b -> Vec3d.ofCenter(b).isInRange(pos, 5)).
                            forEach(this::harvestCrop);
            case 1 -> {//select
                boolean found = false;
                if (BlockPos.streamOutwards(blockPos, 5, 5, 5).
                        anyMatch(b -> world.getBlockState(b).getBlock() == Blocks.FARMLAND)) {
                    if (selectFarmlandItem()) {
                        found = true;
                    }
                    farmland = true;
                }
                if (!found && BlockPos.streamOutwards(blockPos, 5, 5, 5).
                        anyMatch(b -> world.getBlockState(b).getBlock() == Blocks.SOUL_SAND)) {
                    if (selectNetherwartItem()) {
                        found = true;
                    }
                    farmland = false;
                }
                if (!found)
                    cycle = 0;
            }
            case 2 -> {//plant
                Block filter = farmland ? Blocks.FARMLAND : Blocks.SOUL_SAND;
                BlockPos.streamOutwards(blockPos, 5, 5, 5).
                        filter(b -> world.getBlockState(b).getBlock() == filter).
                        filter(b -> world.getBlockState(b.up()).getBlock() == Blocks.AIR).
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
        if (!cropBlock.isMature(state))
            return;
        attackPos(pos);
    }

    private void harvestNetherWarts(BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof NetherWartBlock))
            return;
        if (state.get(Properties.AGE_3) < 3)
            return;
        attackPos(pos);
    }

    private void sameBelow(BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!sameBelow.contains(block))
            return;
        if (world.getBlockState(pos.down()).getBlock() != block)
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
        ClientPlayerEntity player = PlayerUtils.getPlayer();
        PlayerInventory inventory = player.getInventory();
        inventory.addPickBlock(item.getDefaultStack());
        return inventory.getMainHandStack().getItem() == item;
    }

    private void attackPos(BlockPos target) {
        ClientPlayerEntity player = PlayerUtils.getPlayer();
        ClientPlayNetworkHandler nh = MinecraftClient.getInstance().getNetworkHandler();
        if (nh == null)
            return;
        nh.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, target, getHitDirectionForBlock(player, target)));
        nh.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, target, getHitDirectionForBlock(player, target)));
    }

    private void clickPos(Vec3i target) {
        BlockPos bp = new BlockPos(target);
        Direction dir = Direction.UP;
        PlayerInteractBlockC2SPacket packet = new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(bp.offset(dir)), dir, bp, false));
        ClientPlayNetworkHandler nh = MinecraftClient.getInstance().getNetworkHandler();
        if (nh == null) return;
        nh.sendPacket(packet);
    }
}
