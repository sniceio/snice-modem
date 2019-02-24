package io.snice.modem.actors.fsm;

public enum ModemState {
    CONNECTING, CONNECTED, IDENTIFICATION, RESET, READY, DISCONNECTING, TERMINATED;
}
