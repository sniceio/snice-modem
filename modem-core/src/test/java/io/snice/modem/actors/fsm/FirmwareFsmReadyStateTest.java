package io.snice.modem.actors.fsm;

import io.snice.modem.actors.events.AtCommand;
import io.snice.modem.actors.messages.modem.ModemResetRequest;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import java.util.List;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
        var cmd = AtCommand.of("AT+COPS=?");
        fsm.onEvent(cmd);
        verify(ctx).writeToModem(cmd);
        assertThat(fsm.getState(), is(FirmwareState.WAIT));

        // for every AT command we write, we will schedule a timeout
        // so that if the command doesn't complete, we have to abort
        verify(scheduler).schedule(any(), any());
    }

    /**
     * If we do not have any reset commands configured then when we
     * receive a RESET command, nothing will really happen.
     *
     * @throws Exception
     */
    @Test
    public void testResetNoResetCommandsConfigured() throws Exception {
        init(List.of());
        fsm.onEvent(ModemResetRequest.of());
        assertThat(fsm.getState(), is(FirmwareState.READY));
        verify(ctx, never()).writeToModem(any());
    }


}
