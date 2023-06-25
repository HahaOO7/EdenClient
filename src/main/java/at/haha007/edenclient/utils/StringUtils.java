package at.haha007.edenclient.utils;

import java.net.SocketAddress;

public class StringUtils {

    public static String getWorldOrServerName() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.hasSingleplayerServer()) {
            net.minecraft.client.server.IntegratedServer server = mc.getSingleplayerServer();

            if (server != null) {
                return server.getWorldData().getLevelName();
            }
        } else {
            net.minecraft.client.multiplayer.ServerData server = mc.getCurrentServer();

            if (server != null) {
                return server.ip.replace(':', '_');
            } else {
                net.minecraft.client.multiplayer.ClientPacketListener handler = mc.getConnection();
                net.minecraft.network.Connection connection = handler != null ? handler.getConnection() : null;

                if (connection != null) {
                    return "realms_" + stringifyAddress(connection.getRemoteAddress());
                }
            }
        }

        return null;
    }

    public static String stringifyAddress(SocketAddress address) {
        String str = address.toString();

        if (str.contains("/")) {
            str = str.substring(str.indexOf('/') + 1);
        }

        return str.replace(':', '_');
    }
}
