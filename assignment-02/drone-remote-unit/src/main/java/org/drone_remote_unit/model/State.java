package org.drone_remote_unit.model;

import com.google.common.primitives.UnsignedBytes;

/**
 * This enum represent the possible remote states.
 */
public enum State {
    /**
     * Init.
     */
    INIT(1),
    /**
     * Idle.
     */
    IDLE(2),
    /**
     * TakeOff.
     */
    TAKEOFF(3),
    /**
     * DroneOut.
     */
    DRONE_OUT(4),
    /**
     * Landing.
     */
    LANDING(5),
    /**
     * PreAlarm.
     */
    PRE_ALARM(6),
    /**
     * Alarm.
     */
    ALARM(7);

    private final byte value; // Unsigned byte

    /**
     * @param value a value between {0, 1, 2, 3, 4, 5, 6}
     * @throws IllegalArgumentException if value is negative or greater than 255
     */
    State(final int value) {
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
     * Return the correct {@code State} corresponding to the given byte.
     *
     * @param b the byte to convert
     * @return the corresponding {@code State}
     * @throws IllegalArgumentException if the parameter is invalid
     */
    public static State fromByte(final byte b) {
        for (final State cmd : values()) {
            if (cmd.value == b) {
                return cmd;
            }
        }
        throw new IllegalArgumentException("Unknown state byte: " + b);
    }

    /**
     * Return the String representation of the given State.
     *
     * @param state the state
     * @return string representation of the command
     */
    public static String toString(final State state) {
        switch (state) {
            case INIT:
                return "Init";
            case IDLE:
                return "Idle";
            case TAKEOFF:
                return "TakeOff";
            case DRONE_OUT:
                return "DroneOut";
            case LANDING:
                return "Landing";
            case PRE_ALARM:
                return "PreAlarm";
            case ALARM:
                return "Alarm";
        }
        throw new IllegalArgumentException("Invalid State value: " + state.getInt());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toString(this);
    }
}
