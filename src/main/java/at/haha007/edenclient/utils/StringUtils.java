package at.haha007.edenclient.utils;

import java.net.SocketAddress;

public class StringUtils {

    public static String getWorldOrServerName() {
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();

        if (mc.isIntegratedServerRunning()) {
            net.minecraft.server.integrated.IntegratedServer server = mc.getServer();

            if (server != null) {
                return server.getSaveProperties().getLevelName();
            }
        } else {
            net.minecraft.client.network.ServerInfo server = mc.getCurrentServerEntry();

            if (server != null) {
                return server.address.replace(':', '_');
            } else {
                net.minecraft.client.network.ClientPlayNetworkHandler handler = mc.getNetworkHandler();
                net.minecraft.network.ClientConnection connection = handler != null ? handler.getConnection() : null;

                if (connection != null) {
                    return "realms_" + stringifyAddress(connection.getAddress());
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
