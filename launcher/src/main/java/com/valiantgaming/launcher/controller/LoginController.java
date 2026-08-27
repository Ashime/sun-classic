package com.valiantgaming.launcher.controller;

import com.valiantgaming.launcher.network.PendingLoginRequest;
import com.valiantgaming.launcher.network.packet.server.AskAuthUser;
import com.valiantgaming.launcher.network.session.ClientSession;
import com.valiantgaming.launcher.network.session.ClientSessionManager;
import com.valiantgaming.launcher.util.WindowDragSupport;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Controller for {@code /fxml/login.fxml}, the modal opened by
 * {@link LauncherController#onLogin}.
 *
 * <p>On {@link #onSubmit}, sends {@code U2A_askAuthUser} over the launcher's existing
 * {@code NioClient} connection to AuthServer (see {@code AskAuthUser}), TEA-encrypting the
 * password with the key {@code ClientPacketHandler} captured from {@code A2U_ansReady} at
 * connect time. The reply ({@code A2U_ansAuthUser}) arrives asynchronously on the Netty event
 * loop thread, so it's correlated back here via {@link PendingLoginRequest}'s single-slot
 * future rather than blocking this method.
 *
 * <p>That TEA key arrives asynchronously too, right after the connection opens - but the
 * launcher window is already interactive by then (see {@code NioClient}'s class comment on
 * why connecting is non-blocking), so a user can reach this form and submit before it lands.
 * {@link #awaitTeaKeyThenLogin} polls briefly for it rather than failing immediately on what's
 * usually just a few hundred milliseconds of startup latency.
 */
@Log4j2
public class LoginController
{
    private static final int LOGIN_TIMEOUT_SECONDS = 10;
    private static final int TEA_KEY_WAIT_TIMEOUT_MILLIS = 5000;
    private static final int TEA_KEY_POLL_INTERVAL_MILLIS = 200;

    @FXML
    private StackPane loginRoot;
    @FXML
    private HBox loginTopBar;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label statusLabel;
    @FXML
    private Button submitButton;

    @FXML
    private void initialize()
    {
        WindowDragSupport.enable(loginTopBar, loginRoot);
    }

    @FXML
    private void onSubmit(ActionEvent event)
    {
        // JavaFX's default uncaught-exception handling for FX-thread event handlers doesn't
        // reliably reach the log4j-routed log file (it can go to raw stderr instead) - wrapped
        // so a bug here is never silently invisible in a log capture.
        try
        {
            doSubmit();
        }
        catch(Exception e)
        {
            log.error("Unexpected error handling login submit", e);
            submitButton.setDisable(false);
            submitButton.setText("LOGIN");
            showStatus("Unexpected error - see launcher log.", false);
        }
    }

    private void doSubmit()
    {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if(username.isEmpty() || password.isEmpty())
        {
            showStatus("Please enter your username and password.", false);
            return;
        }

        ClientSession session = ClientSessionManager.getInstance().getSession();
        boolean channelActive = session != null && session.getCtx() != null && session.getCtx().channel().isActive();
        byte[] teaKeyAtClick = session != null ? session.getTeaKey() : null;

        log.info("Login submitted for '{}' - sessionPresent={}, channelActive={}, teaKeyPresent={}",
                username, session != null, channelActive, teaKeyAtClick != null && teaKeyAtClick.length > 0);

        if(session == null || !channelActive)
        {
            showStatus("Not connected to AuthServer. Please try again shortly.", false);
            return;
        }

        submitButton.setDisable(true);
        submitButton.setText("LOGGING IN...");
        showStatus("Logging in...", true);

        awaitTeaKeyThenLogin(session, username, password, 0);
    }

    private void awaitTeaKeyThenLogin(ClientSession session, String username, String password, int elapsedMillis)
    {
        byte[] teaKey = session.getTeaKey();

        if(teaKey != null && teaKey.length > 0)
        {
            sendLogin(session, username, password, teaKey);
            return;
        }

        if(elapsedMillis >= TEA_KEY_WAIT_TIMEOUT_MILLIS)
        {
            boolean channelActive = session.getCtx() != null && session.getCtx().channel().isActive();
            log.warn("Login for '{}' timed out after {}ms waiting for AuthServer's TEA key - channelActive={}",
                    username, elapsedMillis, channelActive);

            submitButton.setDisable(false);
            submitButton.setText("LOGIN");
            showStatus("Could not establish a secure connection with AuthServer. Please try again.", false);
            return;
        }

        PauseTransition pause = new PauseTransition(Duration.millis(TEA_KEY_POLL_INTERVAL_MILLIS));
        pause.setOnFinished(event ->
        {
            try
            {
                awaitTeaKeyThenLogin(session, username, password, elapsedMillis + TEA_KEY_POLL_INTERVAL_MILLIS);
            }
            catch(Exception e)
            {
                log.error("Unexpected error while waiting for AuthServer's TEA key", e);
                submitButton.setDisable(false);
                submitButton.setText("LOGIN");
                showStatus("Unexpected error - see launcher log.", false);
            }
        });
        pause.play();
    }

    private void sendLogin(ClientSession session, String username, String password, byte[] teaKey)
    {
        log.info("Sending U2A_askAuthUser for '{}'", username);

        CompletableFuture<Boolean> future = PendingLoginRequest.register();
        byte[] packet = new AskAuthUser().createPacket(username, password, teaKey);
        session.getCtx().writeAndFlush(packet);

        future.orTimeout(LOGIN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((authenticated, error) -> Platform.runLater(() ->
                {
                    submitButton.setDisable(false);
                    submitButton.setText("LOGIN");

                    if(error != null)
                    {
                        log.warn("Login request for '{}' timed out waiting for AuthServer", username, error);
                        showStatus("Login timed out. Please try again.", false);
                        return;
                    }

                    if(authenticated)
                    {
                        showStatus("Login successful!", true);
                        log.info("User '{}' logged in successfully", username);

                        // Recorded on the session rather than handed straight to LauncherController:
                        // this modal is opened with showAndWait() and keeps no reference back to its
                        // opener, so the launcher reads the result off the session once we close.
                        session.setUsername(username);
                        // Held for the client handoff on START GAME - see ClientSession#password
                        // for why the password rather than a real token.
                        session.setPassword(password);
                        session.setAuthenticated(true);

                        // Brief pause so "Login successful!" is actually readable before the
                        // modal disappears and the launcher shows the signed-in username.
                        PauseTransition close = new PauseTransition(Duration.millis(700));
                        close.setOnFinished(closeEvent -> ((Stage) loginRoot.getScene().getWindow()).close());
                        close.play();

                        // TODO: hand off to the client bootstrap flow once it exists (see LauncherController#onStartGame).
                    }
                    else
                    {
                        showStatus("Invalid username or password.", false);
                    }
                }));
    }

    @FXML
    private void onForgotCredentials(ActionEvent event)
    {
        // TODO: no account-recovery flow implemented yet.
        log.info("Forgot credentials clicked (UI only, no recovery flow yet)");
    }

    private void showStatus(String message, boolean success)
    {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("form-status-success");

        if(success)
            statusLabel.getStyleClass().add("form-status-success");

        statusLabel.setManaged(true);
        statusLabel.setVisible(true);
    }

    @FXML
    private void onClose(ActionEvent event)
    {
        ((Stage) loginRoot.getScene().getWindow()).close();
    }
}
