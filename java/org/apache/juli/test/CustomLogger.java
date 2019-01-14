package org.apache.juli.test;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Created by JasonFitch on 1/8/2019.
 */
public class CustomLogger extends Logger {
    @Override
    public void log(LogRecord record) {
        getLocation();
        LogMessage(record);

    }

    private void getLocation() {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        String className = stackTrace[0].getClassName();
        try {
            Class<?> aClass = Class.forName(className);
            boolean assignableFrom = Logger.class.isAssignableFrom(aClass);
            System.out.println(assignableFrom);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void LogMessage(LogRecord record) {
        super.log(record);
    }

    /**
     * Protected method to construct a logger for a named subsystem.
     * <p>
     * The logger will be initially configured with a null Level
     * and with useParentHandlers set to true.
     *
     * @param name               A name for the logger.  This should
     *                           be a dot-separated name and should normally
     *                           be based on the package name or class name
     *                           of the subsystem, such as java.net
     *                           or javax.swing.  It may be null for anonymous Loggers.
     * @param resourceBundleName name of ResourceBundle to be used for localizing
     *                           messages for this logger.  May be null if none
     *                           of the messages require localization.
     * @throws MissingResourceException if the resourceBundleName is non-null and
     *                                  no corresponding resource can be found.
     */
    protected CustomLogger(String name, String resourceBundleName) {
        super(name, resourceBundleName);
    }
}
