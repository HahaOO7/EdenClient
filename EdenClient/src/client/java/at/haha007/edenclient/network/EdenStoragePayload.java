package at.haha007.edenclient.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public record EdenStoragePayload(String esPayloadType, byte[] data) implements CustomPacketPayload {

    private static final Identifier PAYLOAD_ID = Identifier.fromNamespaceAndPath("edenstorage", "edenstorage");
    // CustomPacketPayload.Type is used instead of CustomPayload.Id
    public static final CustomPacketPayload.Type<EdenStoragePayload> TYPE = new CustomPacketPayload.Type<>(PAYLOAD_ID);


    // PacketCodec.of becomes StreamCodec.of
    public static final StreamCodec<RegistryFriendlyByteBuf, EdenStoragePayload> CODEC = StreamCodec.of(
            (buf, value) -> {
                byte[] stringBytes = value.esPayloadType.getBytes(StandardCharsets.UTF_8);
                buf.writeShort(stringBytes.length);
                buf.writeBytes(stringBytes);
                buf.writeBytes(value.data);
            },
            buf -> {
                short length = buf.readShort();
                byte[] bytes = new byte[length];
                buf.readBytes(bytes);
                String type = new String(bytes, StandardCharsets.UTF_8);
                byte[] data = new byte[buf.readableBytes()];
                buf.readBytes(data);
                return new EdenStoragePayload(type, data);
            }
    );

    @Override
    @NonNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
