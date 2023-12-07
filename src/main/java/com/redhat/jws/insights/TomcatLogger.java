/* Copyright (C) Red Hat 2023 */
package com.redhat.jws.insights;

import com.redhat.insights.logging.InsightsLogger;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class TomcatLogger implements InsightsLogger {

    private static final Log log = LogFactory.getLog(TomcatLogger.class);

    @Override
    public void debug(String message) {
        log.debug(message);
    }

    @Override
    public void debug(String message, Throwable throwable) {
        log.debug(message, throwable);
    }

    @Override
    public void error(String message) {
        log.error(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        log.error(message, throwable);
    }

    @Override
    public void info(String message) {
        log.info(message);
    }

    @Override
    public void warning(String message) {
        log.warn(message);
    }

    @Override
    public void warning(String message, Throwable throwable) {
        log.warn(message, throwable);
    }

}
