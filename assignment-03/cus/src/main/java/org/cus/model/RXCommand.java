package org.cus.model;

import com.google.common.primitives.UnsignedBytes;

/**
 * This enum represents the possible received commands.
 */
public enum RXCommand {
    /**
     * State.
     */
    STATE(1),
    /**
     * Opening.
     */
    OPENING(2),
    /**
     * Opening level acknowledged.
     */
    ACK_OPENING(4),
    /**
     * "Level meter unconnected" acknowledged.
     */
    ACK_UNCONNECTED(8),
    /**
     * "Level meter connected" acknowledged.
     */
    ACK_CONNECTED(16),
    /**
     * Invalid.
     */
    INVALID(127);

    private final byte value;

    /**
     * @param value a value between {0, 1, 2, 4, 8, 16}
     * @throws IllegalArgumentException if value is negative or greater than 255
     */
    RXCommand(final int value) {
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
     * Return the correct {@code RXCommand} corresponding to the given byte.
     *
     * @param b the byte to convert
     * @return the corresponding {@code RXCommand}
     * @throws IllegalArgumentException if the parameter is invalid
     */
    public static RXCommand fromByte(final byte b) {
        for (final RXCommand cmd : values()) {
            if (cmd.value == b) {
                return cmd;
            }
        }
        throw new IllegalArgumentException("Unknown command byte: " + b);
    }

    /**
     * Maps the given {@code TXCommand} with the right {@code RXCommand}.
     *
     * @param cmd the command to convert
     * @return the corresponding value
     */
    public static RXCommand expectedFrom(final TXCommand cmd) {
        switch (cmd) {
            case GET_STATE:
                return STATE;
            case GET_OPENING:
                return OPENING;
            case SET_OPENING:
                return ACK_OPENING;
            case SET_UNCONNECTED:
                return ACK_UNCONNECTED;
            case SET_CONNECTED:
                return ACK_CONNECTED;
        }
        throw new IllegalArgumentException("Invalid TXCommand value: " + cmd.getInt());
    }

    /**
     * Return the String representation of the given RXCommand.
     *
     * @param cmd the command
     * @return string representation of the command
     */
    public static String toString(final RXCommand cmd) {
        switch (cmd) {
            case INVALID:
                return "Invalid";
            case STATE:
                return "State";
            case OPENING:
                return "Opening";
            case ACK_OPENING:
                return "AckOpening";
            case ACK_UNCONNECTED:
                return "AckUnconnected";
            case ACK_CONNECTED:
                return "AckConnected";
        }
        throw new IllegalArgumentException("Invalid RXCommand value: " + cmd.getInt());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toString(this);
    }
}
