package org.apache.juli.test;

import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.apache.juli.async.AsyncDirectJDKLog;
import org.apache.juli.logging.DirectJDKLog;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * Created by JasonFitch on 12/29/2018.
 */
public class LoggerTest {


    static {
        System.setProperty("AsyncDirectJDKLog", "true");
        System.setProperty("java.util.logging.config.file",
                "F:/JetBrains/IntelliJ IDEA/apache-tomcat-8.5.37-src/output/build/conf/logging.properties");
        System.setProperty("java.util.logging.manager","org.apache.juli.ClassLoaderLogManager");
    }


    public static void main(String[] args) throws InterruptedException {

        String LoggerName = LoggerTest.class.getName();
        System.out.println("LoggerName:" + LoggerName);

        Log logger = LogFactory.getLog(LoggerName);
        Throwable throwable = new Throwable("testThrow");
        logger.info("testMessageWithThrow", throwable);

        Log loggerWithSource = LogFactory.getLog("loggerWithSource");
        loggerWithSource.info("testMessageWithSource");



        //防止第二条日志丢失
        TimeUnit.MILLISECONDS.sleep(1000 * 60 * 30);

    }
}
