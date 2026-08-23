package com.valiantgaming.launcher.util;

import javafx.scene.Node;
import javafx.stage.Stage;

/**
 * Lets a JavaFX {@link Node} (typically a custom top bar) drag its containing
 * {@link Stage} around the screen.
 *
 * <p>Every launcher window uses {@code StageStyle.TRANSPARENT} (see
 * {@code LauncherApplication}/{@code LauncherController#openModal}), which removes the OS
 * window chrome and, with it, the native title bar drag behavior. Each FXML screen draws
 * its own top bar in its place, so dragging has to be reimplemented in code: record the
 * mouse position within the scene on press, then on drag move the stage by the delta
 * between the new screen position and that recorded offset.
 */
public final class WindowDragSupport
{
    private WindowDragSupport()
    {
    }

    /**
     * Wires {@code dragHandle} so pressing and dragging it moves {@code windowRoot}'s
     * containing stage. {@code windowRoot} only needs to already be attached to a scene by
     * the time the user drags (i.e. it must be resolvable from an FXML {@code initialize()}
     * method) - the stage itself is looked up lazily on each drag event.
     *
     * @param dragHandle the node that receives the mouse press/drag (e.g. a top bar HBox)
     * @param windowRoot any node in the same scene as the stage being dragged
     */
    public static void enable(Node dragHandle, Node windowRoot)
    {
        double[] offset = new double[2];

        dragHandle.setOnMousePressed(event ->
        {
            offset[0] = event.getSceneX();
            offset[1] = event.getSceneY();
        });

        dragHandle.setOnMouseDragged(event ->
        {
            Stage stage = (Stage) windowRoot.getScene().getWindow();
            stage.setX(event.getScreenX() - offset[0]);
            stage.setY(event.getScreenY() - offset[1]);
        });
    }
}
