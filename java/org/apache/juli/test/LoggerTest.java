package org.apache.juli.test;

import java.util.concurrent.TimeUnit;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * Created by JasonFitch on 12/29/2018.
 */
public class LoggerTest {


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
