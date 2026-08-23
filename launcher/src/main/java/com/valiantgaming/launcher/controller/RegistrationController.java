package com.valiantgaming.launcher.controller;

import com.valiantgaming.launcher.util.WindowDragSupport;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;

/**
 * Controller for {@code /fxml/registration.fxml}, the modal opened by
 * {@link LauncherController#onRegistration}.
 *
 * <p>Populates the date-of-birth month/day dropdowns at init time (year isn't modeled -
 * only month/day fields exist in the FXML). Like {@link LoginController}, {@link #onSubmit}
 * is UI-only: there is no registration endpoint on auth-server/database-server yet to
 * submit this form to.
 */
@Log4j2
public class RegistrationController
{
    private static final String[] MONTHS = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    @FXML
    private StackPane regRoot;
    @FXML
    private HBox regTopBar;
    @FXML
    private ComboBox<String> dobMonth;
    @FXML
    private ComboBox<Integer> dobDay;

    @FXML
    private void initialize()
    {
        dobMonth.getItems().addAll(MONTHS);
        for(int day = 1; day <= 31; day++)
            dobDay.getItems().add(day);

        WindowDragSupport.enable(regTopBar, regRoot);
    }

    @FXML
    private void onSubmit(ActionEvent event)
    {
        // TODO: UI only for now - there's no registration endpoint on auth-server/
        // database-server yet to submit this to.
        log.info("Registration form submitted (UI only, not wired to a backend yet)");
    }

    @FXML
    private void onClose(ActionEvent event)
    {
        ((Stage) regRoot.getScene().getWindow()).close();
    }
}
