package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.PlayerInteractBlockCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import static at.haha007.edenclient.command.CommandManager.*;

@Mod
public class PlaceDirection {
    @ConfigSubscriber
    private boolean enabled = false;
    @ConfigSubscriber
    private Direction direction = Direction.NORTH;

    public PlaceDirection() {
        PlayerInteractBlockCallback.EVENT.register(this::onInteractBlock, getClass());
        PerWorldConfig.get().register(this, "placeDirection");
        registerCommand();
    }


    private void registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> cmd = literal("eplacedirection");
        cmd.then(literal("toggle").executes(_ -> {
            enabled = !enabled;
            PlayerUtils.sendModMessage(enabled ? "PlaceDirection enabled" : "PlaceDirection disabled");
            return 1;
        }));
        cmd.then(literal("direction").then(argument("direction", StringArgumentType.word())
                .suggests((_, b) -> {
                    for (Direction d : Direction.values()) {
                        b.suggest(d.getName());
                    }
                    return b.buildFuture();
                })
                .executes(c -> {
                    String name = c.getArgument("direction", String.class);
                    Direction dir = Direction.byName(name);
                    if (dir == null) {
                        PlayerUtils.sendModMessage("Unknown direction: " + name);
                        return 0;
                    }
                    direction = dir;
                    PlayerUtils.sendModMessage("PlaceDirection direction set to " + dir.getName());
                    return 1;
                })));

        register(cmd, "PlaceDirection forces blocks to be placed facing a fixed direction.");
    }

    private boolean isPlacingBlock(BlockHitResult hitResult, ClientLevel world, LocalPlayer player, InteractionHand hand) {
        if (hitResult == null || world == null || player == null) {
            return false;
        }
        if (player.isSpectator()) {
            return false;
        }

        ItemStack item = hand == InteractionHand.MAIN_HAND ? player.getMainHandItem() : player.getOffhandItem();
        if (item.isEmpty() || !(item.getItem() instanceof BlockItem)) {
            return false;
        }
        if (player.isShiftKeyDown()) {
            return true;
        }

        BlockPos pos = hitResult.getBlockPos();
        return !isInteractiveBlock(world.getBlockState(pos), world, pos);
    }

    private static boolean isInteractiveBlock(BlockState state, ClientLevel world, BlockPos pos) {
        if (state.hasBlockEntity() && world.getBlockEntity(pos) instanceof MenuProvider) {
            return true;
        }
        return state.is(BlockTags.BUTTONS) || state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS)
                || state.is(BlockTags.FENCE_GATES) || state.is(BlockTags.BEDS) || state.is(BlockTags.ANVIL)
                || state.is(BlockTags.ALL_SIGNS)
                || isToggledOrOpenedBlock(state.getBlock())
                || isUtilityBlock(state.getBlock());
    }

    private static boolean isToggledOrOpenedBlock(Block block) {
        return block instanceof ButtonBlock || block instanceof LeverBlock || block instanceof NoteBlock
                || block instanceof BellBlock || block instanceof ComparatorBlock
                || block instanceof RepeaterBlock || block instanceof DaylightDetectorBlock;
    }

    private static boolean isUtilityBlock(Block block) {
        return block instanceof EnchantingTableBlock || block instanceof EnderChestBlock
                || block instanceof StructureBlock || block instanceof CraftingTableBlock
                || block instanceof CartographyTableBlock || block instanceof GrindstoneBlock || block instanceof LoomBlock
                || block instanceof SmithingTableBlock || block instanceof StonecutterBlock;
    }

    private InteractionResult onInteractBlock(LocalPlayer player, ClientLevel world, InteractionHand hand, BlockHitResult hitResult) {
        if (!enabled || !isPlacingBlock(hitResult, world, player, hand) || hitResult.getDirection() == direction) {
            return InteractionResult.PASS;
        }
        clickPos(hitResult.getBlockPos().offset(hitResult.getDirection().getUnitVec3i()));
        return InteractionResult.FAIL;
    }

    private void clickPos(Vec3i target) {
        BlockPos bp = new BlockPos(target);
        MultiPlayerGameMode im = Minecraft.getInstance().gameMode;
        if (im == null) {
            return;
        }
        Vec3 opposite = direction.getUnitVec3();
        Vec3 middle = Vec3.atCenterOf(bp).add(opposite.x() / 2, opposite.y() / 2, opposite.z() / 2);

        BlockHitResult hitResult = new BlockHitResult(middle, direction, bp, false);
        LocalPlayer player = PlayerUtils.getPlayer();
        player.swing(InteractionHand.MAIN_HAND, false);
        im.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);
    }

}
