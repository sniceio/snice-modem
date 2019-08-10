package io.snice.modem.actors.messages.modem;

/**
 * Ask the modem for all the data it has in its buffer. Good for debugging
 * when trying to figure out why the modem is (probably) stuck in the WAIT <-> PROCESS loop
 */
public interface BufferPeekRequest extends ModemRequest {
}
