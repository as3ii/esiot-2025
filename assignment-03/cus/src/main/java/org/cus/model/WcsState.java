package org.cus.model;

import com.google.common.primitives.UnsignedBytes;

/**
 * This enum represent the possible WCS states.
 */
public enum WcsState {
    /**
     * Manual.
     */
    MANUAL(1),
    /**
     * Automatic.
     */
    AUTOMATIC(2);

    private final byte value; // Unsigned byte

    /**
     * @param value a value between {0, 1, 2, 3, 4, 5, 6}
     * @throws IllegalArgumentException if value is negative or greater than 255
     */
    WcsState(final int value) {
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
     * Return the correct {@code WcsState} corresponding to the given byte.
     *
     * @param b the byte to convert
     * @return the corresponding {@code WcsState}
     * @throws IllegalArgumentException if the parameter is invalid
     */
    public static WcsState fromByte(final byte b) {
        for (final WcsState cmd : values()) {
            if (cmd.value == b) {
                return cmd;
            }
        }
        throw new IllegalArgumentException("Unknown state byte: " + b);
    }

    /**
     * Return the String representation of the given WcsState.
     *
     * @param state the state
     * @return string representation of the state
     */
    public static String toString(final WcsState state) {
        switch (state) {
            case MANUAL:
                return "Manual";
            case AUTOMATIC:
                return "Automatic";
        }
        throw new IllegalArgumentException("Invalid WcsState value: " + state.getInt());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toString(this);
    }
}
