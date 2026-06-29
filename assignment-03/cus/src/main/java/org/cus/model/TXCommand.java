package org.cus.model;

import com.google.common.primitives.UnsignedBytes;

/**
 * This enum represents the possible commands to send.
 */
public enum TXCommand {
    /**
     * Get state.
     */
    GET_STATE(1),
    /**
     * Request current opening.
     */
    GET_OPENING(2),
    /**
     * Set valve opening.
     */
    SET_OPENING(4),
    /**
     * Tell the device that the level meter is unconnected.
     */
    SET_UNCONNECTED(8),
    /**
     * Tell the device that the level meter is connected.
     */
    SET_CONNECTED(16);

    private final byte value; // Unsigned byte

    /**
     * @param value a value between {1, 2, 4, 8, 16}
     * @throws IllegalArgumentException if value is negative or greater than 255
     */
    TXCommand(final int value) {
        this.value = UnsignedBytes.checkedCast(value);
    }

    /**
     * @return value sent/received via serial interface (unsigned byte)
     */
    public byte getValue() {
        return this.value;
    }

    /**
     * @return the value as an integer
     */
    public int getInt() {
        return UnsignedBytes.toInt(value);
    }

    /**
     * Return the correct {@code TXCommand} corresponding to the given byte.
     *
     * @param b the byte to convert
     * @return the corresponding {@code TXCommand}
     * @throws IllegalArgumentException if the parameter is invalid
     */
    public static TXCommand fromByte(final byte b) {
        for (final TXCommand cmd : values()) {
            if (cmd.value == b) {
                return cmd;
            }
        }
        throw new IllegalArgumentException("Unknown command byte: " + b);
    }

    /**
     * Return the String representation of the given TXCommand.
     *
     * @param cmd the command
     * @return string representation of the command
     */
    public static String toString(final TXCommand cmd) {
        switch (cmd) {
            case GET_STATE:
                return "GetState";
            case GET_OPENING:
                return "GetOpening";
            case SET_OPENING:
                return "SetOpening";
            case SET_UNCONNECTED:
                return "SetUnconnected";
            case SET_CONNECTED:
                return "SetConnected";
        }
        throw new IllegalArgumentException("Invalid TXCommand value: " + cmd.getInt());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toString(this);
    }
}
