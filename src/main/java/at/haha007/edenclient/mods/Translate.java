package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import static at.haha007.edenclient.command.CommandManager.argument;
import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class Translate {
    private final Map<String, String> supportedLanguages = new HashMap<>();
    private final Cache<String, String> cache = CacheBuilder.newBuilder().maximumSize(1000).build();
    private final Cache<String, PropLang> languageCache = CacheBuilder.newBuilder().maximumSize(1000).build();
    private long lockedUntil = 0L;
    private final long minTimeout = 1000;

    @ConfigSubscriber("50")
    private int minConfidence = 50;
    @ConfigSubscriber("en")
    private String targetLanguage = "de";
    @ConfigSubscriber("3")
    private int minLength = 3;
    @ConfigSubscriber("false")
    private boolean enabled;

    public Translate() {
        try {
            JsonArray json = JsonParser.parseString(new String(new URL("http://trans.zillyhuhn.com/languages").openStream().readAllBytes())).getAsJsonArray();
            for (JsonElement lang : json) {
                String code = lang.getAsJsonObject().get("code").getAsString();
                String name = lang.getAsJsonObject().get("name").getAsString();
                supportedLanguages.put(code, name);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        AddChatMessageCallback.EVENT.register(this::onChat);
        PerWorldConfig.get().register(this, "translate");
        registerCommand();
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientCommandSource> command = literal("etranslate");

        command.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendModMessage(enabled ? "Translate enabled" : "Translate disabled");
            return 1;
        }));

        command.then(literal("minLength").
                then(argument("length", IntegerArgumentType.integer(0, 255)).executes(c -> {
                    minLength = c.getArgument("length", Integer.class);
                    return 1;
                })));

        command.then(literal("confidence").executes(c -> {
            sendModMessage("Minimum confidence is " + minConfidence);
            return 1;
        }).then(argument("min", IntegerArgumentType.integer(1, 100)).executes(c -> {
            minConfidence = c.getArgument("min", Integer.class);
            sendModMessage("Minimum confidence is " + minConfidence);
            return 1;
        })));

        command.then(literal("language").executes(c -> {
            sendModMessage("Target language is: " + supportedLanguages.get(targetLanguage));
            return 1;
        }).then(argument("lang", StringArgumentType.word()).suggests((c, s) -> {
            supportedLanguages.keySet().forEach(s::suggest);
            return s.buildFuture();
        }).executes(c -> {
            String s = c.getArgument("lang", String.class);
            if (!supportedLanguages.containsKey(s)) {
                return -1;
            }
            cache.invalidateAll();
            targetLanguage = s;
            sendModMessage("Target language is: " + supportedLanguages.get(targetLanguage));
            return 1;
        })));


        CommandManager.register(command);
    }

    private void onChat(AddChatMessageCallback.ChatAddEvent event) {
        Text text = event.getChatText();
        PropLang lang = detectLanguage(text.asString());
        if (lang == null || lang.language.equals(targetLanguage))
            return;
        text = translate(text, lang);
        event.setChatText(text);
    }

    private Text translate(Text text, PropLang lang) {
        text.getSiblings().replaceAll(s -> translate(s, lang));
        if (!(text instanceof LiteralText literal))
            return text;
        String s = literal.asString();
        if (lang == null)
            return text;
        if (lang.confidence < minConfidence)
            return text;
        s = translate(s, lang);
        LiteralText replacement = new LiteralText(s);
        replacement.setStyle(text.getStyle());
        replacement.getSiblings().addAll(text.getSiblings());
        return replacement;
    }

    private String translate(String text, PropLang lang) {
        String to = cache.getIfPresent(text);
        if (to != null)
            return to;
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("q", text);
        requestJson.addProperty("source", lang.language);
        requestJson.addProperty("target", targetLanguage);
        requestJson.addProperty("format", "text");

        long now = System.currentTimeMillis();
        long delta = lockedUntil - now;
        if (delta > 0) {
            try {
                Thread.sleep(delta);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lockedUntil = now + minTimeout;
        }

        try {
            URL url = new URL("http://trans.zillyhuhn.com/translate");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.getOutputStream().write(requestJson.toString().getBytes());
            connection.connect();
            JsonObject json = JsonParser.parseString(new String(connection.getInputStream().readAllBytes())).getAsJsonObject();
            connection.disconnect();
            String value = json.get("translatedText").getAsString();
            cache.put(text, value);
            return value;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private PropLang detectLanguage(String text) {
        PropLang cacheLang = languageCache.getIfPresent(text);
        if (cacheLang != null)
            return cacheLang;
        if (text.length() < minLength)
            return null;
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("q", text);
        long now = System.currentTimeMillis();
        long delta = lockedUntil - now;
        if (delta > 0) {
            try {
                Thread.sleep(delta);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lockedUntil = now + minTimeout;
        }
        try {
            URL url = new URL("http://trans.zillyhuhn.com/detect");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.getOutputStream().write(requestJson.toString().getBytes());
            connection.connect();
            JsonArray json = JsonParser.parseString(new String(connection.getInputStream().readAllBytes())).getAsJsonArray();
            List<PropLang> probabilities = new ArrayList<>();
            for (JsonElement lang : json) {
                String code = lang.getAsJsonObject().get("language").getAsString();
                int prop = lang.getAsJsonObject().get("confidence").getAsInt();
                probabilities.add(new PropLang(prop, code));
            }
            probabilities.sort(Comparator.comparingInt(p -> p.confidence));
            connection.disconnect();
            cacheLang = probabilities.get(probabilities.size() - 1);
            languageCache.put(text, cacheLang);
            return cacheLang;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private record PropLang(int confidence, String language) {
    }
}
