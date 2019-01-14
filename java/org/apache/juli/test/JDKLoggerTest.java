package org.apache.juli.test;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Created by JasonFitch on 1/14/2019.
 */
public class JDKLoggerTest {

    static {
        System.setProperty("java.util.logging.manager", "org.apache.juli.test.CustomLogManager");
    }

    public static void main(String[] args) {

        String LoggerName = LoggerTest.class.getName();
        System.out.println("LoggerName:" + LoggerName);

        Logger jdklogger1 = Logger.getLogger(LoggerName);
        jdklogger1.setLevel(Level.FINEST);
        LogRecord logRecord = new LogRecord(Level.INFO, "logRecord message");

        //all entrance，actual log
        jdklogger1.log(logRecord);
//        java.lang.Thread.State: RUNNABLE
//        at java.util.logging.Logger.log(Logger.java:720)
//        at org.apache.juli.test.LoggerTest.main(LoggerTest.java:36)

        //other entrance，invoke Logger.log(LogRecord)
        jdklogger1.log(Level.INFO, "log message");
//        java.lang.Thread.State: RUNNABLE
//        at java.util.logging.Logger.log(Logger.java:720)
//        at java.util.logging.Logger.doLog(Logger.java:765)
//        at java.util.logging.Logger.log(Logger.java:788)
//        at org.apache.juli.test.LoggerTest.main(LoggerTest.java:38)

        jdklogger1.logp(Level.INFO, "sourceclass", "sourcemethod", "logp message");
//        java.lang.Thread.State: RUNNABLE
//        at java.util.logging.Logger.log(Logger.java:720)
//        at java.util.logging.Logger.doLog(Logger.java:765)
//        at java.util.logging.Logger.logp(Logger.java:931)
//        at org.apache.juli.test.LoggerTest.main(LoggerTest.java:39)

        jdklogger1.info("info message");
//        java.lang.Thread.State: RUNNABLE
//        at java.util.logging.Logger.log(Logger.java:720)
//        at java.util.logging.Logger.doLog(Logger.java:765)
//        at java.util.logging.Logger.log(Logger.java:788)
//        at java.util.logging.Logger.info(Logger.java:1490)
//        at org.apache.juli.test.LoggerTest.main(LoggerTest.java:40)

        jdklogger1.entering("sourceclass", "sourcemethod", "enter message");
//        java.lang.Thread.State: RUNNABLE
//        at java.util.logging.Logger.log(Logger.java:720)
//        at java.util.logging.Logger.doLog(Logger.java:765)
//        at java.util.logging.Logger.logp(Logger.java:985)
//        at java.util.logging.Logger.entering(Logger.java:1358)
//        at org.apache.juli.test.LoggerTest.main(LoggerTest.java:41)


        //JF 直接调用new Logger 是不会触发LogManager的LogManager.addLogger()方法的。
        CustomLogger customLogger = new CustomLogger("customLogger", null);
        customLogger.setLevel(Level.ALL);
        customLogger.log(logRecord);
        customLogger.log(Level.INFO, "message");
        customLogger.info("info message");
        customLogger.logp(Level.INFO, "sourceClass", "sourceMethod", "info message");
        customLogger.entering("sourceClass", "sourceMethod", "info message");



    }
}

