package io.snice.modem.actors.fsm;

import io.hektor.core.ActorPath;
import io.hektor.core.LifecycleEvent;
import io.hektor.core.internal.Terminated;
import io.snice.modem.actors.events.AtCommand;
import io.snice.modem.actors.messages.modem.ModemResetRequest;
import org.junit.Test;

import java.util.List;

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
        final var cmd = AtCommand.of("AT+COPS=?");
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

    /**
     * When a child terminates, which can only be one of the two actors that is connected to the underlying
     * serial port, then we'll just give up and transition to terminated, which will kill our state machine
     * too, which then the ModemActor will get and can deal with.
     *
     * @throws Exception
     */
    @Test
    public void testChildTermination() {
        assertState(FirmwareState.READY);
        fsm.onEvent(LifecycleEvent.terminated(ActorPath.of("whatever")));
        assertState(FirmwareState.TERMINATED);
    }


}
