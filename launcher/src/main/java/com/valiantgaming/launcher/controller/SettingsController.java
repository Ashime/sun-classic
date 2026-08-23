package com.valiantgaming.launcher.controller;

import com.valiantgaming.launcher.util.WindowDragSupport;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;

/**
 * Controller for {@code /fxml/settings.fxml}, the modal opened by
 * {@link LauncherController#onSettings}.
 *
 * <p>UI-only: {@link #onConfirm} doesn't persist anything yet since there's no local
 * graphics/client settings store for the selector/toggle values to write to.
 */
@Log4j2
public class SettingsController
{
    @FXML
    private StackPane settingsRoot;
    @FXML
    private HBox settingsTopBar;

    @FXML
    private void initialize()
    {
        WindowDragSupport.enable(settingsTopBar, settingsRoot);
    }

    @FXML
    private void onClose(ActionEvent event)
    {
        ((Stage) settingsRoot.getScene().getWindow()).close();
    }

    @FXML
    private void onConfirm(ActionEvent event)
    {
        // TODO: UI only for now - selector/toggle values aren't backed by real graphics config yet.
        log.info("Settings confirmed (UI only, not wired to a backend yet)");
        ((Stage) settingsRoot.getScene().getWindow()).close();
    }

    @FXML
    private void onCancel(ActionEvent event)
    {
        ((Stage) settingsRoot.getScene().getWindow()).close();
    }
}
