package org.apache.juli.test;

import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Created by JasonFitch on 1/8/2019.
 */
public class CustomLogManager extends LogManager{
    @Override
    public boolean addLogger(Logger logger) {
        return super.addLogger(logger);
    }

    @Override
    public Logger getLogger(String name) {
        return super.getLogger(name);
    }
}
