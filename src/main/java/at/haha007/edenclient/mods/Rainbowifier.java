package at.haha007.edenclient.mods;

import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.util.List;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;


public class Rainbowifier {
    private final List<Character> simpleRainbowColors = List.of('c', '6', 'e', 'a', 'b', '3', 'd', '5', 'd', '3', 'b', 'a', 'e', '6');
    @ConfigSubscriber("0.3")
    private double freq = 0.3;

    public Rainbowifier() {
        registerCommand();
        PerWorldConfig.get().register(this, "rainbowify");
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientCommandSource> node = literal("erainbowify");

        node.then(literal("fancy").then(argument("input", StringArgumentType.greedyString()).executes(c -> {
            String input = c.getArgument("input", String.class);
            String message = rainbowifyMessageFancy(input);
            if (message.length() >= 256) {
                sendModMessage(new LiteralText("Your message is too long.").formatted(Formatting.GOLD));
                return 0;
            }
            ClientPlayerEntity entityPlayer = PlayerUtils.getPlayer();
            entityPlayer.sendChatMessage(rainbowifyMessageFancy(input));
            return 1;
        })));

        node.then(literal("simple").then(argument("input", StringArgumentType.greedyString()).executes(c -> {
            String input = c.getArgument("input", String.class);
            String message = rainbowifyMessageSimple(input);
            if (message.length() >= 256) {
                sendModMessage(new LiteralText("Your message is too long.").formatted(Formatting.GOLD));
                return 0;
            }
            ClientPlayerEntity entityPlayer = PlayerUtils.getPlayer();
            entityPlayer.sendChatMessage(rainbowifyMessageSimple(input));
            return 1;
        })));

        node.then(literal("msg").then(argument("player", StringArgumentType.word()).then(argument("message", StringArgumentType.greedyString()).executes(c -> {
            String player = c.getArgument("player", String.class);
            String originalMessage = c.getArgument("message", String.class);
            String message = "/msg " + player + " " + rainbowifyMessageFancy(originalMessage);
            if (message.length() >= 256) {
                message = "/msg " + player + " " + rainbowifyMessageSimple(originalMessage);
                if (message.length() >= 256) {
                    sendModMessage(new LiteralText("Your message is too long.").formatted(Formatting.GOLD));
                    return 0;
                }
            }
            ClientPlayerEntity entityPlayer = PlayerUtils.getPlayer();
            entityPlayer.sendChatMessage(message);
            return 1;
        }))));

        node.then(literal("auto").then(argument("input", StringArgumentType.greedyString()).executes(c -> {
            String input = c.getArgument("input", String.class);
            String message = rainbowifyMessageFancy(input);
            if (message.length() >= 256) {
                message = rainbowifyMessageSimple(input);
                if (message.length() >= 256) {
                    sendModMessage(new LiteralText("Your message is too long.").formatted(Formatting.GOLD));
                    return 0;
                }
            }
            ClientPlayerEntity entityPlayer = PlayerUtils.getPlayer();
            entityPlayer.sendChatMessage(message);
            return 1;
        })));

        node.then(literal("freq").then(argument("frequency", DoubleArgumentType.doubleArg(0.1, 1.0)).executes(c -> {
            this.freq = c.getArgument("frequency", Double.class);
            sendModMessage(new LiteralText("Frequency updated to ").formatted(Formatting.GOLD).append(new LiteralText("" + freq).formatted(Formatting.AQUA)));
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
