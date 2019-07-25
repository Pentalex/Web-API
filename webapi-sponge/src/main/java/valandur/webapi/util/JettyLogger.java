package valandur.webapi.util;

import org.eclipse.jetty.util.log.Logger;
import valandur.webapi.WebAPI;

public class JettyLogger implements Logger {
    private org.slf4j.Logger logger;
    private boolean enableDebug;

    public JettyLogger() {
        logger = WebAPI.getInstance().getLogger();
        enableDebug = WebAPI.isDevMode();
    }

    @Override public String getName() { return "webapi-jetty"; }
    @Override public void warn(String msg, Object... args) {
        logger.warn(msg, args);
    }
    @Override public void warn(Throwable thrown) {
        logger.warn("THROWN", thrown);
    }
    @Override public void warn(String msg, Throwable thrown) {
        logger.warn(msg, thrown);
    }
    @Override public void info(String msg, Object... args) {
        logger.info(msg, args);
    }
    @Override public void info(Throwable thrown) {
        logger.info("THROWN", thrown);
    }
    @Override public void info(String msg, Throwable thrown) {
        logger.info(msg, thrown);
    }
    @Override public boolean isDebugEnabled() { return enableDebug; }
    @Override public void setDebugEnabled(boolean enabled) {
        this.enableDebug = enabled;
    }
    @Override public void debug(String msg, Object... args) {
        logger.debug(msg, args);
    }
    @Override public void debug(String msg, long value) {
        logger.debug(msg, value);
    }
    @Override public void debug(Throwable thrown) {
        logger.debug("THROWN", thrown);
    }
    @Override public void debug(String msg, Throwable thrown) {
        logger.debug(msg, thrown);
    }
    @Override public Logger getLogger(String name) { return this; }
    @Override public void ignore(Throwable ignored) { logger.debug("THROWN", ignored); }
}
