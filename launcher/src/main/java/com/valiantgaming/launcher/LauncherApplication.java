package com.valiantgaming.launcher;

import com.valiantgaming.launcher.config.LauncherConfig;
import com.valiantgaming.launcher.server.NioClient;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.log4j.Log4j2;

/**
 * JavaFX entry point for the launcher's main window. Loads {@code /fxml/launcher.fxml}
 * (backed by {@link com.valiantgaming.launcher.controller.LauncherController}) into a
 * borderless ({@code StageStyle.TRANSPARENT}) stage sized/titled from
 * {@link LauncherConfig}.
 *
 * <p>Launched via {@link com.valiantgaming.launcher.Main#main} rather than directly, so the
 * class carrying the packaged jar's {@code Main-Class} doesn't itself extend
 * {@link Application} - see {@code Main}'s comment for why that split matters.
 */
@Log4j2
public class LauncherApplication extends Application
{
    /**
     * Eagerly initializes {@link LauncherConfig} before the FXML/scene are built, since
     * {@link #start} reads config values (window size/title) while constructing the stage.
     *
     * <p>Also kicks off {@link NioClient}'s (async, non-blocking) connection attempt to
     * AuthServer - see that class's comment for why a failed connection here doesn't stop
     * the window from opening.
     */
    @Override
    public void init()
    {
        LauncherConfig.getInstance();
        NioClient.getInstance();
    }

    @Override
    public void start(Stage stage) throws Exception
    {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/launcher.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, LauncherConfig.getWindowWidth(), LauncherConfig.getWindowHeight());
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/css/launcher.css").toExternalForm());

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle(LauncherConfig.getWindowTitle());
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/logo.png")));
        stage.setResizable(false);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();

        log.info("Launcher started ({})", LauncherConfig.getLauncherVersion());
    }

    @Override
    public void stop()
    {
        NioClient.getInstance().stop();
        log.info("Launcher shutting down");
    }
}
