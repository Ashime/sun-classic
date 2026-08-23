package com.valiantgaming.launcher;

import javafx.application.Application;

/**
 * Packaged jar's {@code Main-Class} (see launcher's {@code pom.xml} spring-boot-maven-plugin
 * / javafx-maven-plugin config).
 *
 * <p>Kept separate from {@link com.valiantgaming.launcher.LauncherApplication} so the
 * fat-jar's {@code Main-Class} does not itself extend {@link Application} - {@code java -jar}
 * refuses to start otherwise, reporting missing JavaFX runtime components.
 */
public class Main
{
    public static void main(String[] args)
    {
        Application.launch(LauncherApplication.class, args);
    }
}
