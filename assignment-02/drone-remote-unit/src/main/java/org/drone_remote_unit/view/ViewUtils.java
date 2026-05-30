package org.drone_remote_unit.view;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Collection of utilites for the view.
 */
public final class ViewUtils {

    private ViewUtils() {
        throw new UnsupportedOperationException(
            "Utility class and cannot be instantiated"
        );
    }

    /**
     * Tests if a CharSequence is empty {@code ""}, null, or contains only
     * whitespace as defined by {@link Character#isWhitespace(char)}.
     * Original: https://commons.apache.org/proper/commons-lang/apidocs/src-html/org/apache/commons/lang3/StringUtils.html
     *
     * <pre>
     * isBlank(null)      = true
     * isBlank("")        = true
     * isBlank(" ")       = true
     * isBlank("bob")     = false
     * isBlank("  bob  ") = false
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is null, empty or whitespace only
     */
    public static boolean isBlank(final CharSequence cs) {
        final int strLen = cs == null ? 0 : cs.length();
        if (strLen == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Custom form builder.
     */
    public static class Form extends JPanel {

        public static final long serialVersionUID = 92778569L;
        private static final int PADDING = 5;
        private static final int FONT_SIZE = 14;

        private final GridBagConstraints gbc;
        private int row;

        /**
         * Custom Form constructor.
         */
        public Form() {
            super(new GridBagLayout());
            gbc = new GridBagConstraints();
            gbc.insets = new Insets(PADDING, PADDING, PADDING, PADDING);
            row = 0;
        }

        /**
         * Add a line to the form, with a label followed by a field in the same line.
         *
         * @param labelText the label to put on the left
         * @param field the {@code JComponent} to put on the right
         */
        public void addField(final String labelText, final JComponent field) {
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.EAST;
            final JLabel label = new JLabel(labelText);
            label.setFont(new Font("SansSerif", Font.PLAIN, FONT_SIZE));
            label.setHorizontalAlignment(SwingConstants.RIGHT);
            this.add(label, gbc);

            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            this.add(field, gbc);
            row++;
        }

        /**
         * Add a line to the form, with the JComponent spanning the entire line.
         *
         * @param component the {@code JComponent} to add
         */
        public void addCenterComponent(final JComponent component) {
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            this.add(component, gbc);
            row++;
        }

        /**
         * Add a line to the form, with the JComponent on the left.
         *
         * @param component the {@code JComponent} to add on the left
         */
        public void addLeftComponent(final JComponent component) {
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            this.add(component, gbc);
            row++;
        }

        /**
         * Add a line to the form, with the JComponent on the right.
         *
         * @param component the {@code JComponent} to add on the right
         */
        public void addRightComponent(final JComponent component) {
            gbc.gridx = 1;
            gbc.gridy = row;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            this.add(component, gbc);
            row++;
        }
    }
}
