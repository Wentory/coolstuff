package com.wentory.coolstuff.config;

import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Values which must be known before NeoForge freezes the entity registry. */
public final class BootstrapConfig {
    private static final String CONTENT = readConfig();
    public static final boolean ULTRA_GHAST = readMechanic("ultraGhast");
    public static final boolean SPORE_CREEPER = readMechanic("sporeCreeper");
    public static final boolean FROSTLING = readMechanic("frostling");
    public static final boolean ZOMBIE_WOLF = readMechanic("zombieWolf");
    public static final boolean CAKE_FILLINGS = readMechanic("cakeFillings");

    private BootstrapConfig() {}

    private static String readConfig() {
        Path path = FMLPaths.CONFIGDIR.get().resolve("coolstuff-common.toml");
        if (!Files.isRegularFile(path)) return "";
        try { return Files.readString(path); }
        catch (IOException ignored) { return ""; }
    }

    private static boolean readMechanic(String key) {
        Matcher section = Pattern.compile("(?ms)^\\s*\\[mechanics]\\s*$.*?(?=^\\s*\\[|\\z)").matcher(CONTENT);
        if (!section.find()) return true;
        Matcher value = Pattern.compile("(?m)^\\s*" + Pattern.quote(key)
                + "\\s*=\\s*(true|false)\\s*(?:#.*)?$", Pattern.CASE_INSENSITIVE).matcher(section.group());
        return !value.find() || Boolean.parseBoolean(value.group(1));
    }
}
