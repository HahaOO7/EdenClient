package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.ColorUtils;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static at.haha007.edenclient.command.CommandManager.argument;
import static at.haha007.edenclient.command.CommandManager.literal;

@Mod
public class PrintPicture {
    private static final char PIXEL_CHAR = '█';

    @ConfigSubscriber
    private int chatWidth = 24;
    @ConfigSubscriber
    private double rate;
    @ConfigSubscriber
    private boolean legacyFormatting = true;

    public PrintPicture() {
        LiteralArgumentBuilder<FabricClientCommandSource> cmd = literal("eprintpicture");
        cmd = cmd.then(literal("url").then(argument("url", StringArgumentType.greedyString()).executes(c -> {
            String url = c.getArgument("url", String.class);
            new Thread(() -> {
                try {
                    downscaleAndPrint(url);
                } catch (Exception e) {
                    LogUtils.getLogger().error("Error while downloading picture", e);
                    PlayerUtils.sendModMessage(Component.text("Failed to load picture: " + e.getMessage()).color(NamedTextColor.RED));
                }
            }).start();
            return 1;
        })));
        cmd = cmd.then(literal("legacy").executes(c -> {
            legacyFormatting = !legacyFormatting;
            String message = "Legacy formatting is now " + (legacyFormatting ? "enabled" : "disabled") + ".";
            PlayerUtils.sendModMessage(Component.text(message).color(NamedTextColor.GREEN));
            return 1;
        }));
        cmd = cmd.then(literal("width").then(argument("width", IntegerArgumentType.integer(1)).executes(c -> {
            chatWidth = c.getArgument("width", Integer.class);
            String message = "Chat width set to " + chatWidth + ".";
            PlayerUtils.sendModMessage(Component.text(message).color(NamedTextColor.GREEN));
            return 1;
        })));
        cmd = cmd.then(literal("rate").then(argument("rate", DoubleArgumentType.doubleArg(0)).executes(c -> {
            rate = c.getArgument("rate", Double.class);
            String message = "Messages per seconds set to %.1f.".formatted(rate);
            PlayerUtils.sendModMessage(Component.text(message).color(NamedTextColor.GREEN));
            return 1;
        })));
        CommandManager.register(cmd);
    }

    private void downscaleAndPrint(String url) {
        Color[][] picture = loadAndDownscale(url);
        for (Color[] colors : picture) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Color color : colors) {
                String prefix = ColorUtils.colorToLegacyPrefix(color);
                stringBuilder.append('&').append(prefix).append(PIXEL_CHAR);
            }
            PlayerUtils.messageC2S(stringBuilder.toString());
            try {
                Thread.sleep(rate > 0 ? (long) (1000 / rate) : 0);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private Color[][] loadAndDownscale(String url) {
        final BufferedImage image;
        try {
            String source = url.trim();
            URI uri = URI.create(source);
            if (uri.getScheme() == null || uri.getScheme().isBlank()) {
                try (InputStream in = Files.newInputStream(Path.of(source))) {
                    image = ImageIO.read(in);
                }
            } else {
                try (InputStream in = uri.toURL().openStream()) {
                    image = ImageIO.read(in);
                }
            }
        } catch (IllegalArgumentException | IOException e) {
            throw new RuntimeException("Failed to load image from: " + url, e);
        }

        if (image == null) {
            throw new IllegalArgumentException("Unsupported or unreadable image: " + url);
        }
        if (image.getWidth() <= 0 || image.getHeight() <= 0) {
            throw new IllegalArgumentException("Image has invalid dimensions: " + image.getWidth() + "x" + image.getHeight());
        }

        int targetWidth = Math.max(1, chatWidth);
        int targetHeight = Math.max(1, (int) Math.round(image.getHeight() * (targetWidth / (double) image.getWidth())));

        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, targetWidth, targetHeight);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g.dispose();
        }

        Color[][] pixels = new Color[targetHeight][targetWidth];
        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                pixels[y][x] = new Color(scaled.getRGB(x, y));
            }
        }
        return pixels;
    }

}
