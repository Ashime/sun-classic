package com.valiantgaming.launcher.controller;

import com.valiantgaming.launcher.config.LauncherConfig;
import com.valiantgaming.launcher.util.WindowDragSupport;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controller for {@code /fxml/registration.fxml}, the modal opened by
 * {@link LauncherController#onRegistration}.
 *
 * <p>Populates the date-of-birth month/day dropdowns at init time (year isn't modeled -
 * only month/day fields exist in the FXML), then on {@link #onSubmit} POSTs the form as JSON
 * to web-server's {@code /api/register} (see {@code RegistrationController} and
 * {@code RegisterAccountRequest} there) - host/port come from Launcher.ini's
 * {@code [WEB_SERVER]} section. There's no JSON library on this module's classpath, so the
 * request/response bodies are hand-built/parsed here rather than pulling one in for a single
 * flat DTO.
 */
@Log4j2
public class RegistrationController
{
    private static final String[] MONTHS = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final Pattern MESSAGE_PATTERN = Pattern.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    @FXML
    private StackPane regRoot;
    @FXML
    private HBox regTopBar;
    @FXML
    private TextField usernameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private ComboBox<String> dobMonth;
    @FXML
    private ComboBox<Integer> dobDay;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField securityQuestion1Field;
    @FXML
    private TextField securityAnswer1Field;
    @FXML
    private TextField securityQuestion2Field;
    @FXML
    private TextField securityAnswer2Field;
    @FXML
    private TextField securityQuestion3Field;
    @FXML
    private TextField securityAnswer3Field;
    @FXML
    private Label statusLabel;
    @FXML
    private Button submitButton;

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
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String password = passwordField.getText();
        String securityQuestion1 = securityQuestion1Field.getText().trim();
        String securityAnswer1 = securityAnswer1Field.getText().trim();
        String securityQuestion2 = securityQuestion2Field.getText().trim();
        String securityAnswer2 = securityAnswer2Field.getText().trim();
        String securityQuestion3 = securityQuestion3Field.getText().trim();
        String securityAnswer3 = securityAnswer3Field.getText().trim();
        int birthMonthIndex = dobMonth.getSelectionModel().getSelectedIndex();
        Integer birthDay = dobDay.getValue();

        // firstName and the security question/answer pairs are nullable on Profile (see the Profile
        // table's Allow Nulls column) and optional on RegisterAccountRequest to match - only the
        // fields below are actually required to submit the form.
        if(username.isEmpty() || email.isEmpty() || lastName.isEmpty() || password.isEmpty()
                || birthMonthIndex < 0 || birthDay == null)
        {
            showStatus("Please fill in the required fields before submitting.", false);
            return;
        }

        int birthMonth = birthMonthIndex + 1;

        String json = "{"
                + field("username", username) + ","
                + field("password", password) + ","
                + field("email", email) + ","
                + optionalField("firstName", firstName) + ","
                + field("lastName", lastName) + ","
                + "\"birthMonth\":" + birthMonth + ","
                + "\"birthDay\":" + birthDay + ","
                + optionalField("securityQuestion1", securityQuestion1) + ","
                + optionalField("answer1", securityAnswer1) + ","
                + optionalField("securityQuestion2", securityQuestion2) + ","
                + optionalField("answer2", securityAnswer2) + ","
                + optionalField("securityQuestion3", securityQuestion3) + ","
                + optionalField("answer3", securityAnswer3)
                + "}";

        String url = "http://" + LauncherConfig.getWebServerIp() + ":" + LauncherConfig.getWebServerPort() + "/api/register";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        submitButton.setDisable(true);
        submitButton.setText("CREATING...");
        showStatus("Creating account...", true);

        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> Platform.runLater(() ->
                {
                    submitButton.setDisable(false);
                    submitButton.setText("CREATE ACCOUNT");

                    if(error != null)
                    {
                        log.error("Registration request to {} failed", url, error);
                        showStatus("Couldn't reach the server. Please try again.", false);
                        return;
                    }

                    boolean success = response.statusCode() == 201;
                    String message = extractMessage(response.body());

                    showStatus(message != null ? message : "Registration failed (HTTP " + response.statusCode() + ").", success);

                    if(success)
                        log.info("Account '{}' registered successfully", username);
                    else
                        log.warn("Registration for '{}' failed: HTTP {} - {}", username, response.statusCode(), response.body());
                }));
    }

    private static String field(String name, String value)
    {
        return "\"" + name + "\":\"" + escape(value) + "\"";
    }

    /** Like {@link #field}, but an empty value is sent as JSON {@code null} rather than "" - see
     * RegisterAccountRequest, where these fields are nullable rather than merely optional-but-blank. */
    private static String optionalField(String name, String value)
    {
        return value.isEmpty() ? "\"" + name + "\":null" : field(name, value);
    }

    private static String escape(String value)
    {
        StringBuilder builder = new StringBuilder(value.length());

        for(char c : value.toCharArray())
        {
            switch(c)
            {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default ->
                {
                    if(c < 0x20)
                        builder.append(String.format("\\u%04x", (int) c));
                    else
                        builder.append(c);
                }
            }
        }

        return builder.toString();
    }

    /** Pulls {@code message} out of web-server's {@code {"message": "..."}} JSON body. */
    private static String extractMessage(String body)
    {
        if(body == null)
            return null;

        Matcher matcher = MESSAGE_PATTERN.matcher(body);
        if(!matcher.find())
            return null;

        return matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
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
        ((Stage) regRoot.getScene().getWindow()).close();
    }
}
