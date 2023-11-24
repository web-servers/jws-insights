package com.redhat.jws.insights;

import com.redhat.insights.logging.InsightsLogger;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class TomcatLogger implements InsightsLogger {

    private static final Log log = LogFactory.getLog(TomcatLogger.class);

    @Override
    public void debug(String arg0) {
        log.debug(arg0);
    }

    @Override
    public void debug(String arg0, Throwable arg1) {
        log.debug(arg0, arg1);
    }

    @Override
    public void error(String arg0) {
        log.error(arg0);
    }

    @Override
    public void error(String arg0, Throwable arg1) {
        log.error(arg0, arg1);
    }

    @Override
    public void info(String arg0) {
        log.info(arg0);
    }

    @Override
    public void warning(String arg0) {
        log.warn(arg0);
    }

    @Override
    public void warning(String arg0, Throwable arg1) {
        log.warn(arg0, arg1);
    }

}
