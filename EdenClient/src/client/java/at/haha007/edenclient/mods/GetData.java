package at.haha007.edenclient.mods;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.utils.NbtFormatter;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Scheduler;
import at.haha007.edenclient.utils.screen.ShowTextScreen;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.xpple.clientarguments.arguments.CBlockPosArgument;
import dev.xpple.clientarguments.arguments.CEntityArgument;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueOutput;

import static at.haha007.edenclient.command.CommandManager.*;

@Mod
public class GetData {
    public GetData() {
        LiteralArgumentBuilder<FabricClientCommandSource> cmd = literal("egetdata");
        cmd.executes(_ -> {
            PlayerUtils.sendModMessage("/egetdata [entity|block] <id>");
            return 1;
        });
        cmd.then(literal("entity").then(argument("id", CEntityArgument.entity()).executes(ctx -> {
            Entity entity = CEntityArgument.getEntity(ctx, "id");
            showEntityNbtScreen(entity);
            return 1;
        })));
        cmd.then(literal("block").then(argument("id", CBlockPosArgument.blockPos()).executes(ctx -> {
            BlockPos pos = CBlockPosArgument.getBlockPos(ctx, "id");
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return -1;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity == null) {
                PlayerUtils.sendModMessage("No block entity found at " + pos);
                return -1;
            }
            showBlockNbtScreen(blockEntity);
            return 1;
        })));
        register(cmd, "Gets data of an entity or block", "/egetdata [entity|block] <id>");
    }


    public void showEntityNbtScreen(Entity entity) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        if (!entity.save(output)) {
            PlayerUtils.sendModMessage("Could not serialize entity data");
            return;
        }
        CompoundTag tag = output.buildResult();
        Component text = NbtFormatter.format(tag, true, 2, Integer.MAX_VALUE, true);
        ShowTextScreen showTextScreen = new ShowTextScreen(text);
        EdenClient.getMod(Scheduler.class).scheduleSyncDelayed(() -> Minecraft.getInstance().gui.setScreen(showTextScreen), 1);
    }

    public void showBlockNbtScreen(BlockEntity blockEntity) {
        CompoundTag tag = blockEntity.saveWithFullMetadata(PlayerUtils.getPlayer().registryAccess());
        Component text = NbtFormatter.format(tag, true, 2, Integer.MAX_VALUE, true);
        ShowTextScreen showTextScreen = new ShowTextScreen(text);
        EdenClient.getMod(Scheduler.class).scheduleSyncDelayed(() -> Minecraft.getInstance().gui.setScreen(showTextScreen), 1);
    }
}
