package io.snice;

import io.hektor.actors.LoggingSupport;
import io.hektor.core.Actor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShellActor implements Actor, LoggingSupport {

    private static final Logger logger = LoggerFactory.getLogger(ShellActor.class);

    @Override
    public void onReceive(final Object msg) {

    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public Object getUUID() {
        return self();
    }
}
