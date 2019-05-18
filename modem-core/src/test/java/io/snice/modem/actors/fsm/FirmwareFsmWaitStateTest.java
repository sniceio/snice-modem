package io.snice.modem.actors.fsm;

import io.snice.modem.actors.events.TransactionTimeout;
import org.junit.Test;

/**
 * Test all transitions out of the WAIT state.
 */
public class FirmwareFsmWaitStateTest extends FirmwareFsmTestBase {

    /**
     * Ensure that
     *
     * @throws Exception
     */
    @Test
    public void testOnAtCommandTimeout() throws Exception {
        final var cmd = onAtCommand("ATI");
        fsm.onEvent(TransactionTimeout.of(cmd));
        assertState(FirmwareState.READY);
    }


}
