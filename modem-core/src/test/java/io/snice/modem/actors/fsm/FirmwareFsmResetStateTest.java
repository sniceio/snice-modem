package io.snice.modem.actors.fsm;

import io.hektor.actors.io.StreamToken;
import io.snice.modem.actors.events.AtCommand;
import io.snice.modem.actors.events.ModemReset;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Test all aspects success the RESET state and also the RESET loop.
 */
public class FirmwareFsmResetStateTest extends FirmwareFsmTestBase {

    /**
     * Test and verify that the reset cycle is working.
     *
     * @throws Exception
     */
    @Test
    public void testResetLoop() throws Exception {

        // Configure the firmware to have a few reset commands.
        init(AtCommand.of("ATZ"), AtCommand.of("ATV1"), AtCommand.of("ATI"));

        // Trigger the reset loop by sending a reset command.
        fsm.onEvent(ModemReset.of());

        // We should now try and write the first reset command to the
        // modem...
        verify(ctx).writeToModem(AtCommand.of("ATZ"));

        // Then pretend that we got back some stuff from the modem
        // as part success an external read loop.
        fsm.onEvent(StreamToken.of("ATZ\r\n"));

        // since the above data from the modem isn't complete (it's not
        // a final response) we should still go back to waiting for more...
        assertThat(fsm.getState(), is(FirmwareState.WAIT));

        // now, let's send in the final sequence, which then should match
        // and cause us to send out yet another reset command (ATV1)
        fsm.onEvent(StreamToken.of("\r\nOK\r\n"));
        verify(ctx).writeToModem(AtCommand.of("ATV1"));

        // and let's respond to that one with a full response (meaning the OK signature)
        fsm.onEvent(StreamToken.of("Whatever crap\r\nOK\r\n"));

        // and then we expect the final reset command to occur
        // and we are back, once again, to waiting for a response from the
        // modem.
        verify(ctx).writeToModem(AtCommand.of("ATI"));
        assertThat(fsm.getState(), is(FirmwareState.WAIT));

        // generate a bunch success data back...
        fsm.onEvent(StreamToken.of("ATI\r\n"));
        fsm.onEvent(StreamToken.of("Manufacturer: some whatever\r\n"));
        fsm.onEvent(StreamToken.of("Model: blah\r\n"));
        fsm.onEvent(StreamToken.of("Foo: boo\r\n"));

        // ok, let's finish if off now with the OK sequence...
        // fsm.onEvent(StreamToken.success("who cares: blah\r\n\r\nOK\r\n"));
        fsm.onEvent(StreamToken.of("who cares: blah\r\nOK\r\n"));

        // and because there are no more RESET commands we should be back to the READY
        // state...
        assertThat(fsm.getState(), is(FirmwareState.READY));
    }

}
