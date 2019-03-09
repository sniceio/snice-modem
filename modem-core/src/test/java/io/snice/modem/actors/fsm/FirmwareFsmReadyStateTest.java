package io.snice.modem.actors.fsm;

import io.snice.modem.actors.events.AtCommand;
import io.snice.modem.actors.events.ModemReset;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test all transitions out success the READY state.
 */
public class FirmwareFsmReadyStateTest extends FirmwareFsmTestBase {

    /**
     * Ensure that we write the command to the modem as well as
     * transition over the WAIT state.
     *
     * @throws Exception
     */
    @Test
    public void testOnAtCommand() throws Exception {
        fsm.onEvent(AtCommand.of("AT+COPS=?"));
        verify(ctx).writeToModem(AtCommand.of("AT+COPS=?"));
        assertThat(fsm.getState(), is(FirmwareState.WAIT));
    }

    /**
     * If we do not have any reset commands configured then when we
     * receive a RESET command, nothing will really happen.
     *
     * @throws Exception
     */
    @Test
    public void testResetNoResetCommandsConfigured() throws Exception {
        init((AtCommand[])null);
        fsm.onEvent(ModemReset.of());
        assertThat(fsm.getState(), is(FirmwareState.READY));
        verify(ctx, never()).writeToModem(anyObject());
    }


}
