package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

import java.util.List;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;


public class Rainbowifier {
    private final List<Character> simpleRainbowColors = List.of('c', '6', 'e', 'a', 'b', '3', 'd', '5', 'd', '3', 'b', 'a', 'e', '6');
    private double freq;

    public Rainbowifier() {
        registerCommand();
        ConfigSaveCallback.EVENT.register(this::onSave);
        ConfigLoadCallback.EVENT.register(this::onLoad);
    }

    private ActionResult onLoad(NbtCompound nbtCompound) {
        NbtCompound rainbowTag = nbtCompound.getCompound("rainbowify");

        if (rainbowTag != null) {
            if (rainbowTag.contains("freq"))
                this.freq = rainbowTag.getDouble("freq");
            else
                this.freq = 0.3;
        }

        return ActionResult.PASS;
    }

    private ActionResult onSave(NbtCompound nbtCompound) {
        NbtCompound rainbowTag = new NbtCompound();
        rainbowTag.putDouble("freq", this.freq);
        nbtCompound.put("rainbowify", rainbowTag);

        return ActionResult.PASS;
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientCommandSource> node = literal("rainbowify");

        node.then(literal("fancy").then(argument("input", StringArgumentType.greedyString()).executes(c -> {
            String input = c.getArgument("input", String.class);

            String message = rainbowifyMessageFancy(input);

            if (message.length() >= 256) {
                sendModMessage(new LiteralText("Deine Nachricht ist zu lang. Es werden nur bis zu 25 Zeichen (mit einigen Leerzeichen) im Rainbowifier-Fancy unterstützt.").formatted(Formatting.GOLD));
                return 0;
            }
            ClientPlayerEntity entityPlayer = MinecraftClient.getInstance().player;

            if (entityPlayer != null) {
                entityPlayer.sendChatMessage(rainbowifyMessageFancy(input));
            }

            return 1;
        })));

        node.then(literal("simple").then(argument("input", StringArgumentType.greedyString()).executes(c -> {
            String input = c.getArgument("input", String.class);

            String message = rainbowifyMessageSimple(input);

            if (message.length() >= 256) {
                sendModMessage(new LiteralText("Deine Nachricht ist zu lang. Es werden nur bis zu 80 Zeichen (mit einigen Leerzeichen) im Rainbowifier-Simple unterstützt.").formatted(Formatting.GOLD));
                return 0;
            }
            ClientPlayerEntity entityPlayer = MinecraftClient.getInstance().player;

            if (entityPlayer != null) {
                entityPlayer.sendChatMessage(rainbowifyMessageSimple(input));
            }

            return 1;
        })));

        node.then(literal("msg").then(argument("player", StringArgumentType.word()).then(argument("message", StringArgumentType.greedyString()).executes(c -> {
            String player = c.getArgument("player", String.class);
            if (!player.matches("[a-zA-Z0-9_]{1,16}")) {
                sendModMessage(new LiteralText("Der Spielername, den du eingegeben hast, ist ungültig.").formatted(Formatting.GOLD));
                return 0;
            }

            String originalMessage = c.getArgument("message", String.class);

            String message = "/msg " + player + " " + rainbowifyMessageFancy(originalMessage);

            if (message.length() >= 256) {
                message = "/msg " + player + " " + rainbowifyMessageSimple(originalMessage);
                if (message.length() >= 256) {
                    sendModMessage(new LiteralText("Deine Nachricht ist zu lang. Es werden nur bis zu 25 Zeichen im Rainbowifier-Fancy und 80 Zeichen im Rainbowifier-Simple unterstützt.").formatted(Formatting.GOLD));
                    return 0;
                }
            }

            ClientPlayerEntity entityPlayer = MinecraftClient.getInstance().player;

            if (entityPlayer != null) {
                entityPlayer.sendChatMessage(message);
            }

            return 1;
        }))));

        node.then(literal("auto").then(argument("input", StringArgumentType.greedyString()).executes(c -> {
            String input = c.getArgument("input", String.class);
            boolean simple = false;

            String message = rainbowifyMessageFancy(input);

            if (message.length() >= 256) {
                simple = true;
                message = rainbowifyMessageSimple(input);
                if (message.length() >= 256) {
                    sendModMessage(new LiteralText("Deine Nachricht ist zu lang. Es werden nur bis zu 80 Zeichen (mit einigen Leerzeichen) im Rainbowifier-Simple unterstützt.").formatted(Formatting.GOLD));
                    return 0;
                }
            }

            ClientPlayerEntity entityPlayer = MinecraftClient.getInstance().player;

            if (entityPlayer != null) {
                entityPlayer.sendChatMessage(message);
                sendModMessage("Für deine Nachricht wurde der " + (simple ? "simple" : "fancy") + " Modus ausgewählt");
            }

            return 1;
        })));

        node.then(literal("freq").then(argument("frequency", DoubleArgumentType.doubleArg(0.1, 1.0)).executes(c -> {
            this.freq = c.getArgument("frequency", Double.class);
            sendModMessage(new LiteralText("Frequency für Rainbowify-Fancy wurde auf " + freq + " gesetzt.").formatted(Formatting.GOLD));

            return 1;
        })));

        node.executes(c -> {
            sendDebugMessage();
            return 1;
        });

        register(node);
    }

    private String rainbowifyMessageFancy(String input) {
        StringBuilder rainbow = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char character = input.charAt(i);
            int[] color = getFancyRainbowColorAtIndex(i);
            if (character == ' ') {
                rainbow.append(' ');
                continue;
            }
            rainbow.append(String.format("&#%02X%02X%02X", color[0], color[1], color[2])).append(character);
        }

        return rainbow.toString();
    }

    private String rainbowifyMessageSimple(String input) {
        StringBuilder rainbow = new StringBuilder();
        int length = input.length();

        for (int i = 0; i < length; i++) {
            char character = input.charAt(i);
            String color = getSimpleRainbowColorAtIndex(i);

            if (character == ' ') {
                rainbow.append(' ');
                continue;
            }

            rainbow.append(color).append(character);
        }

        return rainbow.toString();
    }

    private int[] getFancyRainbowColorAtIndex(int index) {
        int[] color = new int[3];

        color[0] = (int) (Math.sin(freq * index) * 127 + 128);
        color[1] = (int) (Math.sin(freq * index + 2) * 127 + 128);
        color[2] = (int) (Math.sin(freq * index + 4) * 127 + 128);

        return color;
    }

    private String getSimpleRainbowColorAtIndex(int input) {
        return "&" + this.simpleRainbowColors.get(input % this.simpleRainbowColors.size());
    }

    private void sendDebugMessage() {
        sendModMessage(new LiteralText("/rainbowify [simple, fancy, freq, auto, msg <recipient message>]").formatted(Formatting.GOLD));
    }
}
