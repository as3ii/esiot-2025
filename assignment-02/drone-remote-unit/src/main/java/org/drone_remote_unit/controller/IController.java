package org.drone_remote_unit.controller;

import jssc.SerialPortException;

/**
 * Represents the methods that the controller must expose.
 */
public interface IController {

    /**
     * @param port name of the serial port
     * @param baudRate baud rate
     * @param dataBits data bits per byte
     * @param stopBits stop bits settings
     * @param parity parity settings
     * @throws SerialPortException if exception occurred
     */
    void initSerial(
        String port, int baudRate, int dataBits, int stopBits, int parity
    ) throws SerialPortException;

    /**
     * Close the serial port.
     *
     * @throws SerialPortException if exception occurred
     */
    void closeSerial() throws SerialPortException;

    /**
     * Check if the serial port is connected.
     *
     * @return true if the connection is established
     */
    boolean isConnected();

    /**
     * Get the system state.
     */
    void queryState();

    /**
     * Send a take off request.
     */
    void requestTakeOff();

    /**
     * Send a landing request.
     */
    void requestLanding();

    /**
     * Query the current temperature.
     */
    void queryTemperature();

    /**
     * Query the drone distance, available only if {@code State} is {@code LANDING}.
     */
    void queryDistance();
}
