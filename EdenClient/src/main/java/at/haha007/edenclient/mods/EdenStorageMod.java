package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.network.EdenStoragePayload;
import at.haha007.edenclient.utils.PlayerUtils;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import dev.xpple.clientarguments.arguments.CBlockPosArgument;
import dev.xpple.clientarguments.arguments.CItemArgument;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;

import static at.haha007.edenclient.command.CommandManager.*;

@Mod
public class EdenStorageMod {
    private static boolean payloadTypeRegistered = false;

    public EdenStorageMod() {
        registerPayloadTypes();

        // 2. Setup the receiver
        ClientPlayNetworking.registerGlobalReceiver(EdenStoragePayload.TYPE, (payload, context) -> {
            String serverMsg = payload.esPayloadType();
            byte[] data = payload.data();
            if (serverMsg.equals("store_data")) {
                storageDataUpdated(data);
            }
        });

        LeaveWorldCallback.EVENT.register(this::leaveWorld, getClass());
        registerCommand();
    }

    private static synchronized void registerPayloadTypes() {
        if (payloadTypeRegistered) return;
        PayloadTypeRegistry.playS2C().register(EdenStoragePayload.TYPE, EdenStoragePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(EdenStoragePayload.TYPE, EdenStoragePayload.CODEC);
        payloadTypeRegistered = true;
    }

    private void registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> request = literal("request")
                .then(argument("world", StringArgumentType.word())
                        .then(argument("pos", CBlockPosArgument.blockPos())
                                .executes(context -> {
                                    String world = StringArgumentType.getString(context, "world");
                                    BlockPos pos = CBlockPosArgument.getBlockPos(context, "pos");
                                    int x = pos.getX();
                                    int y = pos.getY();
                                    int z = pos.getZ();
                                    PlayerUtils.sendModMessage("Requesting storage data from server...");

                                    ByteArrayDataOutput output = ByteStreams.newDataOutput();
                                    output.writeUTF(world);
                                    output.writeInt(x);
                                    output.writeInt(y);
                                    output.writeInt(z);
                                    ClientPlayNetworking.send(new EdenStoragePayload("store_request", output.toByteArray()));
                                    return 1;
                                })));

        ClientPacketListener nh = Minecraft.getInstance().getConnection();
        if (nh == null) {
            LogUtils.getLogger().warn("Connection is null, cannot register commands");
            return;
        }
        CommandBuildContext context = CommandBuildContext.simple(nh.registryAccess(), nh.enabledFeatures());
        LiteralArgumentBuilder<FabricClientCommandSource> swap = literal("swap")
                .then(argument("slot", IntegerArgumentType.integer())
                        .then(argument("item", CItemArgument.itemStack(context))
                                .executes(c -> {
                                    int slot = IntegerArgumentType.getInteger(c, "slot");
                                    ItemInput item = CItemArgument.getItemStackArgument(c, "item");
                                    ItemStack stack = item.createItemStack(1, false);
                                    stack.setCount(stack.getMaxStackSize());

                                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                         DataOutputStream dos = new DataOutputStream(bos)) {
                                        dos.writeInt(slot);
                                        dos.write(itemStackToBytes(stack));
                                        ClientPlayNetworking.send(new EdenStoragePayload("swap_slot", bos.toByteArray()));
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    return 1;
                                })));

        LiteralArgumentBuilder<FabricClientCommandSource> restock = literal("restock")
                .then(argument("slot", IntegerArgumentType.integer())
                        .executes(c -> {
                            int slot = IntegerArgumentType.getInteger(c, "slot");
                            try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                 DataOutputStream dos = new DataOutputStream(bos)) {
                                dos.writeInt(slot);
                                ClientPlayNetworking.send(new EdenStoragePayload("restock_slot", bos.toByteArray()));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            return 1;
                        }));

        LiteralArgumentBuilder<FabricClientCommandSource> node = literal("estorage");
        node.then(request);
        node.then(swap);
        node.then(restock);
        register(node, "Ensures blocks are broken with silk touch.");
    }

    private void leaveWorld() {
        ClientPlayNetworking.unregisterGlobalReceiver(EdenStoragePayload.TYPE.id());
    }


    public static byte[] decompress(byte[] compressedData) throws IOException {
        Inflater inflater = new Inflater();
        inflater.setInput(compressedData);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(compressedData.length);
        byte[] buffer = new byte[1024];

        try {
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
        } catch (DataFormatException e) {
            throw new IOException("Data format exception during decompression", e);
        } finally {
            inflater.end();
        }

        return outputStream.toByteArray();
    }

    private void storageDataUpdated(byte[] data) {
        try {
            DataInputStream is = new DataInputStream(new ByteArrayInputStream(data));
            int length = is.readShort();
            String world = new String(is.readNBytes(length), StandardCharsets.UTF_8);
            int x = is.readInt();
            int y = is.readInt();
            int z = is.readInt();
            byte[] compressedData = is.readAllBytes();
            String yamlString = new String(decompress(compressedData));
            storageDataUpdated(world, x, y, z, yamlString);
        } catch (IOException e) {
            LogUtils.getLogger().warn("Failed to read object from data {}", new BigInteger(data).toString(0x10), e);
        }
    }

    private void storageDataUpdated(String world, int x, int y, int z, String yamlString) {
        // Handle the updated storage data here

        YAMLMapper mapper = new YAMLMapper();
        JsonNode yaml = mapper.readTree(yamlString);
        EdenStorageDrive[] drives = new EdenStorageDrive[6];
        yaml.forEachEntry((driveName, driveData) -> {
            int version = driveData.get("version").asInt();
            int driveIndex = Integer.parseInt(driveName.replaceFirst("drive_", ""));
            Map<ItemStack, Integer> items = new HashMap<>();
            driveData.get("items").forEach(itemData -> {
                ItemStack stack = bytesToItemStack(Base64.getDecoder().decode(itemData.get("item").stringValue()));
                int amount = itemData.get("amount").asInt();
                items.put(stack, amount);
            });
            drives[driveIndex] = new EdenStorageDrive(driveData.get("maxItems").asInt(), version, items);
        });
        EdenStorageSystem system = new EdenStorageSystem(drives);
        LogUtils.getLogger().info("Received storage data for world {} at ({}, {}, {}): \n{}\n", world, x, y, z, system);
    }

    record EdenStorageDrive(int size, int version, Map<ItemStack, Integer> items) {
    }

    record EdenStorageSystem(EdenStorageDrive[] drives) {
        @Override
        @NonNull
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < drives.length; i++) {
                EdenStorageDrive drive = drives[i];
                sb.append("Drive ").append(i).append(": ");
                if (drive == null) {
                    sb.append("null");
                } else {
                    sb.append(drive);
                }
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    private ItemStack bytesToItemStack(byte[] bytes) {
        try {
            CompoundTag nbt = NbtIo.read(new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes))));

            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                LogUtils.getLogger().warn("Level is null, cannot read item stack");
                return ItemStack.EMPTY;
            }
            HolderLookup.Provider registries = level.registryAccess();

            return ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), nbt)
                    .getOrThrow();
        } catch (Throwable e) {
            LogUtils.getLogger().warn("Failed to read item stack from bytes ", e);
            return ItemStack.EMPTY;
        }
    }

    private byte[] itemStackToBytes(ItemStack itemStack) {
        try {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                LogUtils.getLogger().warn("Level is null, cannot write item stack");
                return new byte[0];
            }
            HolderLookup.Provider registries = level.registryAccess();

            CompoundTag tag = ItemStack.CODEC.encode(itemStack, registries.createSerializationContext(NbtOps.INSTANCE), new CompoundTag())
                    .getOrThrow().asCompound().orElseThrow();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            NbtIo.writeCompressed(tag, bos);
            return bos.toByteArray();
        } catch (Throwable e) {
            LogUtils.getLogger().warn("Failed to write item stack to bytes ", e);
            return new byte[0];
        }
    }
}
