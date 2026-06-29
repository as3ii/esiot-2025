package org.cus.controller;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Simple preconfigured logger.
 */
public final class SimpleLogger {

    private static Level defLevel = Level.INFO;

    /**
     * Private constructor.
     */
    private SimpleLogger() {
        throw new UnsupportedOperationException("Utility class and cannot be instantiated");
    }

    /**
     * Set the default log level for new loggers.
     *
     * @param level the level to set
     */
    public static void setLevel(final Level level) {
        defLevel = level;
    }

    /**
     * Get a preconfigured logger that logs in stderr all INFO or above levels.
     *
     * @param name logger name
     * @return preconfigured Logger
     */
    public static Logger getLogger(final String name) {
        return getLogger(name, defLevel);
    }

    /**
     * Get a preconfigured logger that logs in stderr.
     *
     * @param name logger name
     * @param level lowest logged level
     * @return preconfigured Logger
     */
    public static Logger getLogger(final String name, final Level level) {
        final Logger logger = Logger.getLogger(name);
        logger.setLevel(level);
        // Remove parent handlers to avoid duplicate logging
        logger.setUseParentHandlers(false);
        final ConsoleHandler h = new ConsoleHandler();
        h.setLevel(level);
        h.setFormatter(new SimpleFormatter());
        logger.addHandler(h);
        return logger;
    }
}
