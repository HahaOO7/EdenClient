package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.PlayerInteractBlockCallback;
import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class DoubleDoor {

    @ConfigSubscriber("false")
    private boolean enabled = true;

    public DoubleDoor() {
        PlayerInteractBlockCallback.EVENT.register(this::onInteractBlock);
        PerWorldConfig.get().register(this, "doubleDoor");
        registerCommand();
    }

    private void registerCommand() {
        var node = literal("edoubledoor");
        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendModMessage(ChatColor.GOLD + (enabled ? "Enabled DoubleDoor." : "Disabled DoubleDoor."));
            return 1;
        }));

        register(node,
                "DoubleDoor opens multiple doors with one click.");
    }

    private ActionResult onInteractBlock(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockHitResult blockHitResult) {
        if (!enabled) return ActionResult.PASS;
        if (player.isSneaking()) return ActionResult.PASS;
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
        BlockPos bp = blockHitResult.getBlockPos();

        RegistryKey<? extends Registry<Block>> registryKey = BlockTags.DOORS.registry();
        DynamicRegistryManager registryManager = world.getRegistryManager();
        Registry<?> registry = registryManager.get(registryKey);

        if (noDoor(bp, registry, world)) return ActionResult.PASS;


        clickPos(bp.north(), registry, world);
        clickPos(bp.south(), registry, world);
        clickPos(bp.west(), registry, world);
        clickPos(bp.east(), registry, world);

        return ActionResult.PASS;
    }

    private boolean noDoor(BlockPos bp, Registry<?> registry, ClientWorld world) {
        Identifier id = Registries.BLOCK.getId(world.getBlockState(bp).getBlock());
        return !registry.containsId(id);
    }

    private void clickPos(BlockPos target, Registry<?> registry, ClientWorld world) {
        if (noDoor(target, registry, world)) return;
        BlockPos bp = new BlockPos(target);
        Direction dir = Direction.UP;
        var nh = MinecraftClient.getInstance().getNetworkHandler();
        if (nh == null) return;
        nh.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(bp.offset(dir)), dir, bp, false),1));
    }
}
