package io.snice.modem.actors.fsm;

import io.hektor.fsm.Context;
import io.hektor.fsm.Data;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

import static org.mockito.Mockito.mock;

public class FsmTestBase<S extends Enum<S>, C extends Context, D extends Data> {

    private final static Logger logger = LoggerFactory.getLogger(FsmTestBase.class);


    protected final BiConsumer<S, Object> unhandledEventHandler = mock(BiConsumer.class);

    @Before
    public void setup() throws Exception {
    }


    @After
    public void tearDown() throws Exception {
        ensureNoUnhandledEvents();
    }

    /**
     * Unless you are actually testing for dealing with unhandled events, this should never be called
     * so we'll always check it at the end of every test.
     */
    private void ensureNoUnhandledEvents() {
        Mockito.verifyZeroInteractions(unhandledEventHandler);
    }

    public void onTransition(final S currentState, final S toState, final Object event) {
        logger.info("{} -> {} Event: {}", currentState, toState, event);
   }
}
