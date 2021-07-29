package at.haha007.edenclient.utils;

import at.haha007.edenclient.mods.chestshop.ChestShopMod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StringUtils {

    public static String getWorldOrServerName() {
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();

        if (mc.isIntegratedServerRunning()) {
            net.minecraft.server.integrated.IntegratedServer server = mc.getServer();

            if (server != null) {
                return server.getName();
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

    public static List<String> getViableIDs(String path) {
        List<String> minecraftIDs;

        try (InputStream in = ChestShopMod.class.getResourceAsStream(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(in)))) {
            minecraftIDs = reader.lines().map(String::trim).filter(s -> !s.equals("// contains all viable ID's as of version 1.17.1")).collect(Collectors.toList());
        } catch (IOException | NullPointerException e) {
            return null;
        }

        return minecraftIDs;
    }
}
