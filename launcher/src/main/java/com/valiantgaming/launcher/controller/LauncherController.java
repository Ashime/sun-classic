package com.valiantgaming.launcher.controller;

import com.valiantgaming.launcher.config.LauncherConfig;
import com.valiantgaming.launcher.network.session.ClientSession;
import com.valiantgaming.launcher.network.session.ClientSessionManager;
import com.valiantgaming.launcher.util.GameClientLauncher;
import com.valiantgaming.launcher.util.ServerHealthCheck;
import com.valiantgaming.launcher.util.WindowDragSupport;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Controller for {@code /fxml/launcher.fxml}, the launcher's main (non-modal) window.
 *
 * <p>Owns the window chrome (drag, minimize, close), the live server-time clock, the
 * online/offline status dots, and opening the login/registration/settings modals. The
 * "Login Server" dot ({@code connectServerStatus}) is polled live against AuthServer (see
 * {@link #startAuthServerHealthCheck()}); the "Game Server" dot is still driven from the
 * static {@link LauncherConfig#isConnectServerEnabled()} flag since game-server has no
 * real listener yet to check. Each modal has its own FXML +
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

    private static final int AUTH_SERVER_CHECK_INTERVAL_SECONDS = 5;
    private static final int AUTH_SERVER_CHECK_TIMEOUT_MILLIS = 2000;

    @FXML
    private StackPane rootPane;
    @FXML
    private VBox shell;
    @FXML
    private HBox topBar;

    @FXML
    private Label serverTimeValue;
    @FXML
    private HBox loggedInUserRow;
    @FXML
    private Label loggedInUserValue;
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
    private Button registrationButton;
    @FXML
    private Button loginButton;

    @FXML
    private void initialize()
    {
        shell.setClip(buildShellClip());
        wireBeveledOutline(eventsPanelOutline, eventsPanel, EVENTS_PANEL_CORNER_RADIUS, EVENTS_PANEL_BEVEL_SIZE);

        boolean connectServerOn = LauncherConfig.isConnectServerEnabled();
        gameServerStatus.getStyleClass().add(connectServerOn ? "status-online" : "status-offline");

        connectServerStatus.getStyleClass().add("status-offline");
        startAuthServerHealthCheck();

        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> updateServerTime()), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        WindowDragSupport.enable(topBar, rootPane);

        // Signed-out state: REGISTRATION + LOGIN occupy the action row. showLoggedInUser()
        // swaps START GAME in once a login succeeds.
        setButtonShown(startGameButton, false);
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

    /**
     * Polls AuthServer's reachability on a daemon background thread (a raw connect attempt
     * would block the FX Application Thread if run directly from a {@link Timeline}) and
     * reflects the result on {@link #connectServerStatus}. Runs once immediately, then every
     * {@link #AUTH_SERVER_CHECK_INTERVAL_SECONDS} seconds for as long as the launcher is open -
     * the executor's daemon thread doesn't need an explicit shutdown to let the JVM exit.
     */
    private void startAuthServerHealthCheck()
    {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable ->
        {
            Thread thread = new Thread(runnable, "auth-server-health-check");
            thread.setDaemon(true);
            return thread;
        });

        executor.scheduleWithFixedDelay(() ->
        {
            boolean reachable = ServerHealthCheck.isReachable(
                    LauncherConfig.getAuthServerIp(), LauncherConfig.getAuthServerPort(), AUTH_SERVER_CHECK_TIMEOUT_MILLIS);

            Platform.runLater(() ->
            {
                connectServerStatus.getStyleClass().removeAll("status-online", "status-offline");
                connectServerStatus.getStyleClass().add(reachable ? "status-online" : "status-offline");
            });
        }, 0, AUTH_SERVER_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
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
        // openModal blocks (showAndWait) until the login window closes, so by the time it
        // returns the session carries the result of whatever the user did in there.
        openModal("/fxml/login.fxml", "login");
        showLoggedInUser();
    }

    /**
     * Switches the launcher into its signed-in state once {@link LoginController} has
     * authenticated: reveals the username above the preview art, and swaps the action row from
     * REGISTRATION + LOGIN over to START GAME (both of the former are pointless once you're
     * signed in, and START GAME then spans the row on its own).
     *
     * <p>Does nothing if the modal was closed without a successful login, so the launcher stays
     * exactly as it was.
     */
    private void showLoggedInUser()
    {
        ClientSession session = ClientSessionManager.getInstance().getSession();

        if(session == null || !session.isAuthenticated() || session.getUsername() == null)
            return;

        loggedInUserValue.setText(session.getUsername());
        loggedInUserRow.setManaged(true);
        loggedInUserRow.setVisible(true);

        setButtonShown(registrationButton, false);
        setButtonShown(loginButton, false);
        setButtonShown(startGameButton, true);
    }

    /** Shows/hides a button, keeping managed in step with visible so a hidden one leaves no gap
     * in the action row rather than an empty slot. */
    private void setButtonShown(Button button, boolean shown)
    {
        button.setVisible(shown);
        button.setManaged(shown);
    }

    @FXML
    private void onStartGame(ActionEvent event)
    {
        ClientSession session = ClientSessionManager.getInstance().getSession();

        // The button is only shown once signed in (see showLoggedInUser), but re-check rather
        // than trust the UI state - the client is launched with these credentials.
        if(session == null || !session.isAuthenticated() || session.getUsername() == null)
        {
            log.warn("Start Game clicked while not signed in - ignoring.");
            showAlert(Alert.AlertType.WARNING, "Not signed in", "Please log in before starting the game.");
            return;
        }

        String clientPath = LauncherConfig.getClientPath();

        try
        {
            GameClientLauncher.launch(clientPath, session.getUsername(), session.getPassword());

            // Never log the password - see ClientSession#password.
            log.info("Launched game client for '{}' from {}", session.getUsername(), clientPath);

            // Get out of the way rather than exiting: the launcher is the only thing holding the
            // AuthServer connection, and closing it would drop that mid-handoff.
            ((Stage) rootPane.getScene().getWindow()).setIconified(true);
        }
        catch(IOException e)
        {
            log.error("Unable to launch the game client from '{}'", clientPath, e);
            showAlert(Alert.AlertType.ERROR, "Could not start the game",
                    e.getMessage() + "\n\nCheck [CLIENT] PATH in Config/Launcher/Launcher.ini.");
        }
    }

    private void showAlert(Alert.AlertType type, String header, String message)
    {
        Alert alert = new Alert(type);
        alert.initOwner(rootPane.getScene().getWindow());
        alert.setTitle(LauncherConfig.getWindowTitle());
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
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
