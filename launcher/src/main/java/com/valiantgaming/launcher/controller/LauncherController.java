package com.valiantgaming.launcher.controller;

import com.valiantgaming.launcher.config.LauncherConfig;
import com.valiantgaming.launcher.util.WindowDragSupport;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import lombok.extern.log4j.Log4j2;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller for {@code /fxml/launcher.fxml}, the launcher's main (non-modal) window.
 *
 * <p>Owns the window chrome (drag, minimize, close), the live server-time clock, the
 * online/offline status dots (currently driven from the static
 * {@link LauncherConfig#isConnectServerEnabled()} flag rather than a real health check),
 * and opening the login/registration/settings modals. Each modal has its own FXML +
 * controller ({@link LoginController}, {@link RegistrationController},
 * {@link SettingsController}) rather than sharing this one, since they're independent
 * windows with their own lifecycle.
 */
@Log4j2
public class LauncherController
{
    private static final DateTimeFormatter SERVER_TIME_FORMAT = DateTimeFormatter.ofPattern("hh.mm.ss a");

    // Must match the shellOutline Path in launcher.fxml (the visible bevelled panel) so the
    // clip built below lines up with it exactly.
    private static final double SHELL_WIDTH = 1080;
    private static final double SHELL_HEIGHT = 540;
    private static final double SHELL_CORNER_RADIUS = 16;
    private static final double SHELL_BEVEL_SIZE = 44;

    // Same bevel treatment as the shell, scaled down for the events panel (which resizes
    // with the VBox.vgrow layout, so its outline is recomputed at runtime instead of static).
    private static final double EVENTS_PANEL_CORNER_RADIUS = 10;
    private static final double EVENTS_PANEL_BEVEL_SIZE = 20;

    @FXML
    private StackPane rootPane;
    @FXML
    private VBox shell;
    @FXML
    private HBox topBar;

    @FXML
    private Label serverTimeValue;
    @FXML
    private Circle connectServerStatus;
    @FXML
    private Circle gameServerStatus;
    @FXML
    private StackPane eventsPanel;
    @FXML
    private SVGPath eventsPanelOutline;
    @FXML
    private Button startGameButton;

    @FXML
    private void initialize()
    {
        shell.setClip(buildShellClip());
        wireBeveledOutline(eventsPanelOutline, eventsPanel, EVENTS_PANEL_CORNER_RADIUS, EVENTS_PANEL_BEVEL_SIZE);

        boolean connectServerOn = LauncherConfig.isConnectServerEnabled();
        connectServerStatus.getStyleClass().add(connectServerOn ? "status-online" : "status-offline");
        gameServerStatus.getStyleClass().add(connectServerOn ? "status-online" : "status-offline");

        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> updateServerTime()), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        WindowDragSupport.enable(topBar, rootPane);

        // Kept in the FXML (not deleted) so it's a one-line change to bring back once the
        // client bootstrap/login flow is real; loginButton sits in its place until then.
        startGameButton.setVisible(false);
        startGameButton.setManaged(false);
    }

    // Traces the same bevelled-top-right / rounded-elsewhere outline as the shellOutline
    // Path in launcher.fxml, clockwise from just after the top-left corner.
    //
    // This is an SVGPath rather than a Path+ArcTo: a Path that is only ever used as a node's
    // clip (never added to the scene graph as an actual child) does not render its geometry
    // in this JavaFX build - the clip silently hides all content. SVGPath's geometry is
    // parsed from a single content string and doesn't have that dependency.
    private SVGPath buildShellClip()
    {
        SVGPath path = new SVGPath();
        path.setContent(beveledOutlineContent(SHELL_WIDTH, SHELL_HEIGHT, SHELL_CORNER_RADIUS, SHELL_BEVEL_SIZE));
        return path;
    }

    // Redraws outline's geometry to match region's current size whenever it changes, so a
    // bevelled panel can live inside a resizable layout (e.g. a VBox.vgrow child) instead of
    // needing fixed dimensions like the window shell.
    private void wireBeveledOutline(SVGPath outline, Region region, double cornerRadius, double bevelSize)
    {
        Runnable redraw = () ->
        {
            double w = region.getWidth();
            double h = region.getHeight();
            if(w > 0 && h > 0)
                outline.setContent(beveledOutlineContent(w, h, cornerRadius, bevelSize));
        };

        region.widthProperty().addListener((obs, oldVal, newVal) -> redraw.run());
        region.heightProperty().addListener((obs, oldVal, newVal) -> redraw.run());
        redraw.run();
    }

    // Bevelled-top-right / rounded-elsewhere outline, clockwise from just after the top-left
    // corner. Shared by the window shell (buildShellClip, fixed size) and the events panel
    // (wireBeveledOutline, resized at runtime).
    private String beveledOutlineContent(double w, double h, double r, double c)
    {
        return String.format(
                "M%.1f,0 L%.1f,0 L%.1f,%.1f L%.1f,%.1f A%.1f,%.1f 0 0,1 %.1f,%.1f " +
                        "L%.1f,%.1f A%.1f,%.1f 0 0,1 0,%.1f L0,%.1f A%.1f,%.1f 0 0,1 %.1f,0 Z",
                r, w - c, w, c, w, h - r, r, r, w - r, h, r, h, r, r, h - r, r, r, r, r);
    }

    private void updateServerTime()
    {
        serverTimeValue.setText(LocalTime.now().format(SERVER_TIME_FORMAT));
    }

    @FXML
    private void onMinimize(ActionEvent event)
    {
        ((Stage) rootPane.getScene().getWindow()).setIconified(true);
    }

    @FXML
    private void onClose(ActionEvent event)
    {
        Platform.exit();
    }

    @FXML
    private void onSettings(ActionEvent event)
    {
        openModal("/fxml/settings.fxml", "settings");
    }

    @FXML
    private void onDiscord(ActionEvent event)
    {
        openLink(LauncherConfig.getDiscordLink());
    }

    @FXML
    private void onWebsite(ActionEvent event)
    {
        openLink(LauncherConfig.getWebsiteLink());
    }

    @FXML
    private void onFacebook(ActionEvent event)
    {
        openLink(LauncherConfig.getFacebookLink());
    }

    @FXML
    private void onRegistration(ActionEvent event)
    {
        openModal("/fxml/registration.fxml", "registration");
    }

    @FXML
    private void onLogin(ActionEvent event)
    {
        openModal("/fxml/login.fxml", "login");
    }

    @FXML
    private void onStartGame(ActionEvent event)
    {
        // TODO: hand off to the auth-server login flow once the client bootstrap is implemented.
        log.info("Start Game clicked");
    }

    /**
     * Loads {@code fxmlPath} into a new owned, modal, transparent-styled {@link Stage} and
     * blocks until it's closed. {@code windowName} is only used for the log message if
     * loading fails.
     */
    private void openModal(String fxmlPath, String windowName)
    {
        try
        {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initOwner(rootPane.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            scene.getStylesheets().add(getClass().getResource("/css/launcher.css").toExternalForm());

            stage.setScene(scene);
            stage.centerOnScreen();
            stage.showAndWait();
        }
        catch(IOException e)
        {
            log.warn("Unable to open {} window", windowName, e);
        }
    }

    private void openLink(String url)
    {
        try
        {
            Desktop.getDesktop().browse(new URI(url));
        }
        catch(Exception e)
        {
            log.warn("Unable to open link: {}", url, e);
        }
    }
}
