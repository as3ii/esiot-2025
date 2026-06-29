package org.cus.model;

/**
 * This enum represent the possible TMS states.
 */
public enum TmsState {
    /**
     * Connected.
     */
    CONNECTED,
    /**
     * Unconnected.
     */
    UNCONNECTED;

    /**
     * Return the String representation of the given TmsState.
     *
     * @param state the state
     * @return string representation of the state
     */
    public static String toString(final TmsState state) {
        switch (state) {
            case CONNECTED:
                return "Connected";
            case UNCONNECTED:
                return "Unconnected";
        }
        throw new IllegalArgumentException("Invalid TmsState value: " + state.name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toString(this);
    }
}
