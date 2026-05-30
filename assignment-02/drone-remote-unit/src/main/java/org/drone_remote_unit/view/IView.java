package org.drone_remote_unit.view;

import org.drone_remote_unit.controller.IController;
import org.drone_remote_unit.model.State;

/**
 * Represents the methods that the main page must expose.
 */
public interface IView {

    /**
     * Shows or hides this `Window` depending on the value of parameter `b`.
     *
     * @param b if true makes the window visible, otherwise hides the window
     * @see JFrame#setVisible(boolean)
     */
    void setVisible(boolean b);

    /**
     * Register the controller that manages the serial interface.
     *
     * @param controller the controller that manages the serial interface
     */
    void setController(IController controller);

    /**
     * Set the current system {@code State}.
     *
     * @param state the current state
     */
    void setState(State state);

    /**
     * Set the displayed temperature in celsius.
     *
     * @param temperature the temperature to be displayed
     */
    void setTemperature(float temperature);

    /**
     * Set the displayed distance in meters.
     *
     * @param distance the distance to be displayed
     */
    void setDistance(float distance);

    /**
     * Add a line to the end of the console UI.
     *
     * @param text the text to add
     */
    void pushSerialLine(String text);

    /**
     * Show the given message.
     *
     * @param msg the error to show
     * @param isError true if the msg represent an error
     */
    void showAlert(String msg, boolean isError);
}
