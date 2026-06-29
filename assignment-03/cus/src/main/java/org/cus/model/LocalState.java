package org.cus.model;

/**
 * This enum represent the possible local states.
 */
public enum LocalState {
    /**
     * Manual.
     */
    MANUAL,
    /**
     * Automatic.
     */
    AUTOMATIC;

    /**
     * Return the String representation of the given LocalState.
     *
     * @param state the state
     * @return string representation of the state
     */
    public static String toString(final LocalState state) {
        switch (state) {
            case MANUAL:
                return "Manual";
            case AUTOMATIC:
                return "Automatic";
        }
        throw new IllegalArgumentException("Invalid LocalState value: " + state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toString(this);
    }
}
