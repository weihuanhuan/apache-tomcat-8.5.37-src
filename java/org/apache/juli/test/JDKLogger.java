package org.apache.juli.test;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Created by JasonFitch on 1/15/2019.
 */
public class JDKLogger extends Logger {


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
    protected JDKLogger(String name, String resourceBundleName) {
        super(name, resourceBundleName);
        logger = Logger.getLogger(name, resourceBundleName);
    }

    private Logger logger;

    public Logger getInstance(String name,String resourceBundleName) {

        logger = Logger.getLogger(name, resourceBundleName);
        return logger;
    }

    @Override
    public void log(LogRecord record) {
        logger.log(record);
        System.out.println("Log record");
    }

    public static void main(String[] args) {

        LogRecord logRecord = new LogRecord(Level.INFO,"message");

        JDKLogger loggerTest = new JDKLogger("LoggerTest", null);
        loggerTest.log(logRecord);

        Logger instance = loggerTest.getInstance("LoggerTest", null);
        instance.log(logRecord);
    }

}
