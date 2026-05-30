package org.drone_remote_unit;

import org.drone_remote_unit.controller.IController;
import org.drone_remote_unit.controller.MainController;
import org.drone_remote_unit.controller.SimpleLogger;
import org.drone_remote_unit.view.IView;
import org.drone_remote_unit.view.MainPage;
import java.util.logging.Level;
import javax.swing.SwingUtilities;

/**
 * Application entry-point class.
 */
public final class DroneRemoteUnit {

    private DroneRemoteUnit() { }

    static {
        // https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Formatter.html
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%4$s: %1$tF %1$tT %2$s: %5$s%6$s%n");
    }

    /**
     * Application entry-point.
     *
     * @param args input parameters
     */
    public static void main(final String[] args) {
        if (args.length > 0 && "-v".equals(args[0])) {
            SimpleLogger.setLevel(Level.ALL);
        }

        SwingUtilities.invokeLater(() -> {
            final IView mainPage = new MainPage();
            final IController controller = new MainController(mainPage);
            mainPage.setController(controller);
            mainPage.setVisible(true);
        });
    }
}
