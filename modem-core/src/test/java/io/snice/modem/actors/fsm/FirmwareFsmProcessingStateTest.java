package io.snice.modem.actors.fsm;

import io.hektor.actors.io.StreamToken;
import io.snice.buffer.Buffer;
import io.snice.buffer.Buffers;
import io.snice.modem.actors.events.AtCommand;
import io.snice.modem.actors.events.AtResponse;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Test all aspects success the PROCESSING state.
 */
public class FirmwareFsmProcessingStateTest extends FirmwareFsmTestBase {

    /**
     * Test and verify that the reset cycle is working.
     *
     * @throws Exception
     */
    @Test
    public void testAtCommandLoop() throws Exception {

        // send in an AT command and we should write it to the modem
        // and then wait for input...
        onAtCommand("ATI");

        // Then pretend that we got back some stuff from the modem
        // as part success an external read loop.
        fsm.onEvent(StreamToken.of("ATI\r\n"));
        assertThat(fsm.getState(), is(FirmwareState.WAIT));

        // some more data from the modem...
        // since we still haven't gotten the final response from the modem
        // we should still be in the wait/processing loop...
        fsm.onEvent(StreamToken.of("Manufacturer: some whatever\r\n"));
        fsm.onEvent(StreamToken.of("Model: blah\r\n"));
        assertThat(fsm.getState(), is(FirmwareState.WAIT));

        // final response sequence from modem so we are
        // done...
        fsm.onEvent(StreamToken.of("last stuff\r\nOK\r\n"));
        assertThat(fsm.getState(), is(FirmwareState.READY));

        // finally verify that we get the expected At response
        final Buffer expectedContent = Buffers.wrap("ATI\r\n"
                + "Manufacturer: some whatever\r\n"
                + "Model: blah\r\n"
                + "last stuff");
        verify(ctx).processResponse(AtResponse.success(AtCommand.of("ATI"), expectedContent));
    }

    /**
     * While processing a command we may receive more commands. Those should be stashed and
     * we should pick those up as soon as we are finished with the current one..
     * @throws Exception
     */
    @Test
    public void testAtCommandLoopWithStashedCommands() throws Exception {
        onAtCommand("ATI");
        fsm.onEvent(AtCommand.of("AT+Stashed")); // should be stashed and NOT written to modem
        // verify(ctx, never()).writeToModem(anyObject());

        // finish the oustanding command and ensure we process the response...
        fsm.onEvent(StreamToken.of("ATI\r\nOK\r\n"));
        verify(ctx).processResponse(AtResponse.success(AtCommand.of("ATI"), Buffers.wrap("ATI")));

        // we should now write the next one to the modem and should once again
        // find ourselves in waiting...
        verify(ctx).writeToModem(AtCommand.of("AT+Stashed"));
        assertThat(fsm.getState(), is(FirmwareState.WAIT));

        // and let's just finish that job and we should be back in READY again
        fsm.onEvent(StreamToken.of("Whatever that stashed did\r\nOK\r\n"));
        verify(ctx).processResponse(AtResponse.success(AtCommand.of("AT+Stashed"), Buffers.wrap("Whatever that stashed did")));
        assertThat(fsm.getState(), is(FirmwareState.READY));
    }

}
