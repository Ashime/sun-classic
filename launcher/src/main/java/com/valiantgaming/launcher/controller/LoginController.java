package com.valiantgaming.launcher.controller;

import com.valiantgaming.launcher.util.WindowDragSupport;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;

/**
 * Controller for {@code /fxml/login.fxml}, the modal opened by
 * {@link LauncherController#onLogin}.
 *
 * <p>Purely a UI shell today: the form has no client-side validation and {@link #onSubmit}
 * does not talk to auth-server yet, since the game client bootstrap that would follow a
 * successful login isn't implemented. Wire this up once that flow exists.
 */
@Log4j2
public class LoginController
{
    @FXML
    private StackPane loginRoot;
    @FXML
    private HBox loginTopBar;

    @FXML
    private void initialize()
    {
        WindowDragSupport.enable(loginTopBar, loginRoot);
    }

    @FXML
    private void onSubmit(ActionEvent event)
    {
        // TODO: UI only for now - not wired to auth-server's login handshake yet.
        log.info("Login form submitted (UI only, not wired to a backend yet)");
    }

    @FXML
    private void onForgotCredentials(ActionEvent event)
    {
        // TODO: no account-recovery flow implemented yet.
        log.info("Forgot credentials clicked (UI only, no recovery flow yet)");
    }

    @FXML
    private void onClose(ActionEvent event)
    {
        ((Stage) loginRoot.getScene().getWindow()).close();
    }
}
