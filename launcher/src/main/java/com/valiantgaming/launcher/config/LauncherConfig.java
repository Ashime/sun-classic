package com.valiantgaming.launcher.config;

import lombok.Getter;
import lombok.SneakyThrows;
import org.ini4j.Wini;

import java.io.File;

/**
 * Loads {@code Config/Launcher/Launcher.ini} into static fields once at startup, following
 * the same per-server {@code *Config} singleton pattern used by {@code AuthServerConfig}
 * etc. (see the project's CLAUDE.md). Read alongside {@code Launcher.ini} when tracing
 * config-driven launcher behavior.
 *
 * <p>Fields are static (rather than instance fields read through {@link #getInstance()})
 * because JavaFX controllers are instantiated by {@link javafx.fxml.FXMLLoader} with no way
 * to inject this singleton into their constructors, so they read config values directly
 * as static getters (e.g. {@code LauncherConfig.getWindowWidth()}).
 *
 * <p>Not thread-safe against concurrent {@link #getInstance()} first-calls in the general
 * case, but double-checked locking is sufficient here since {@link
 * com.valiantgaming.launcher.LauncherApplication#init()} is the sole caller and JavaFX
 * invokes it once, single-threaded, before any controller can read these fields.
 */
public class LauncherConfig
{
    private static LauncherConfig instance;
    private static final String launcherFile = "Config/Launcher/Launcher.ini";

    // [VERSION]
    @Getter
    private static String launcherVersion;

    // [WINDOW]
    @Getter
    private static String windowTitle;
    @Getter
    private static int windowWidth;
    @Getter
    private static int windowHeight;

    // [LINKS]
    @Getter
    private static String discordLink;
    @Getter
    private static String websiteLink;
    @Getter
    private static String facebookLink;
    @Getter
    private static String registrationLink;

    // [GAME_SERVER]
    @Getter
    private static String gameServerName;
    @Getter
    private static boolean connectServerEnabled;

    // [NETWORK]
    @Getter
    private static int disconnect;

    // [AUTH_SERVER]
    @Getter
    private static String authServerIp;
    @Getter
    private static int authServerPort;
    @Getter
    private static int authServerWorkerThreads;

    @SneakyThrows
    public LauncherConfig()
    {
        Wini ini = new Wini(new File(launcherFile));

        // [VERSION]
        launcherVersion = ini.get("VERSION", "LAUNCHER_VERSION", String.class);

        // [WINDOW]
        windowTitle = ini.get("WINDOW", "TITLE", String.class);
        windowWidth = ini.get("WINDOW", "WIDTH", int.class);
        windowHeight = ini.get("WINDOW", "HEIGHT", int.class);

        // [LINKS]
        discordLink = ini.get("LINKS", "DISCORD", String.class);
        websiteLink = ini.get("LINKS", "WEBSITE", String.class);
        facebookLink = ini.get("LINKS", "FACEBOOK", String.class);
        registrationLink = ini.get("LINKS", "REGISTRATION", String.class);

        // [GAME_SERVER]
        gameServerName = ini.get("GAME_SERVER", "NAME", String.class);
        connectServerEnabled = ini.get("GAME_SERVER", "CONNECT_SERVER_ENABLED", boolean.class);

        // [NETWORK]
        disconnect = ini.get("NETWORK", "DISCONNECT", int.class);

        // [AUTH_SERVER]
        authServerIp = ini.get("AUTH_SERVER", "IP", String.class);
        authServerPort = ini.get("AUTH_SERVER", "PORT", int.class);
        authServerWorkerThreads = ini.get("AUTH_SERVER", "WORKER_THREADS", int.class);

        ini.clear();
    }

    public static LauncherConfig getInstance()
    {
        if(instance == null)
            synchronized (LauncherConfig.class)
            {
                if(instance == null)
                    instance = new LauncherConfig();
            }

        return instance;
    }
}
