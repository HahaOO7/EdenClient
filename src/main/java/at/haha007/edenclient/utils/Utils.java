package at.haha007.edenclient.utils;

import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.net.SocketAddress;

public class Utils {
    public static String getWorldOrServerName() {
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();

        if (mc.isIntegratedServerRunning()) {
            net.minecraft.server.integrated.IntegratedServer server = mc.getServer();

            if (server != null) {
                // This used to be just MinecraftServer::getLevelName().
                // Getting the name would now require an @Accessor for MinecraftServer.field23784
                return server.getName();
            }
        } else {
            net.minecraft.client.network.ServerInfo server = mc.getCurrentServerEntry();

            if (server != null) {
                return server.address.replace(":", "");
            } else {
                net.minecraft.client.network.ClientPlayNetworkHandler handler = mc.getNetworkHandler();
                net.minecraft.network.ClientConnection connection = handler != null ? handler.getConnection() : null;

                if (connection != null) {
                    return "realms" + stringifyAddress(connection.getAddress());
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

        return str.replace(":", "");
    }

    public static File getConfigDirectory() {
        return new File(MinecraftClient.getInstance().runDirectory, "config");
    }
}
