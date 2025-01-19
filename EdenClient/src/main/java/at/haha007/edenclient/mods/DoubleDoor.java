package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.PlayerInteractBlockCallback;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class DoubleDoor {

    @ConfigSubscriber("false")
    private boolean enabled = false;

    public DoubleDoor() {
        PlayerInteractBlockCallback.EVENT.register(this::onInteractBlock, getClass());
        PerWorldConfig.get().register(this, "doubleDoor");
        registerCommand();
    }

    private void registerCommand() {
        var node = literal("edoubledoor");
        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendModMessage((enabled ? "Enabled DoubleDoor." : "Disabled DoubleDoor."));
            return 1;
        }));

        register(node,
                "DoubleDoor opens multiple doors with one click.");
    }

    private InteractionResult onInteractBlock(LocalPlayer player, ClientLevel world, InteractionHand hand, BlockHitResult blockHitResult) {
        onInteract(player, world, hand, blockHitResult);
        return InteractionResult.PASS;
    }

    private void onInteract(LocalPlayer player, ClientLevel world, InteractionHand hand, BlockHitResult blockHitResult) {
        if (!enabled) return;
        if (player.isShiftKeyDown()) return;
        if (hand != InteractionHand.MAIN_HAND) return;
        BlockPos bp = blockHitResult.getBlockPos();

        ResourceKey<? extends Registry<Block>> registryKey = BlockTags.DOORS.registry();
        RegistryAccess registryManager = world.registryAccess();
        Registry<?> registry = registryManager.lookupOrThrow(registryKey);

        if (noDoor(bp, registry, world)) return;

        clickPos(bp.north(), registry, world);
        clickPos(bp.south(), registry, world);
        clickPos(bp.west(), registry, world);
        clickPos(bp.east(), registry, world);
    }

    private boolean noDoor(BlockPos bp, Registry<?> registry, ClientLevel world) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(world.getBlockState(bp).getBlock());
        return !registry.containsKey(id);
    }

    private void clickPos(BlockPos target, Registry<?> registry, ClientLevel world) {
        if (noDoor(target, registry, world)) return;
        BlockPos bp = new BlockPos(target);
        Direction dir = Direction.UP;
        var nh = Minecraft.getInstance().getConnection();
        if (nh == null) return;
        nh.send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(bp.relative(dir)), dir, bp, false), 1));
    }
}
