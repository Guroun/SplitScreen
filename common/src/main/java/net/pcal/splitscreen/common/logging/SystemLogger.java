package net.pcal.splitscreen.common.logging;

import org.slf4j.LoggerFactory;

public interface SystemLogger {

    static SystemLogger syslog() {
        return Singleton.INSTANCE;
    }

    void setForceDebugEnabled(boolean debug);

    void error(String message);

    void error(String message, Throwable t);

    default void error(Throwable e) {
        this.error(e.getMessage(), e);
    }

    void warn(String message);

    void info(String message);

    void debug(String message);

    void debug(String message, Throwable t);

    default void debug(Throwable t) {
        this.debug(t.getMessage(), t);
    }

    class Singleton {
        private static SystemLogger INSTANCE = new Slf4jSystemLogger(LoggerFactory.getLogger("splitscreen"));
    }
}
