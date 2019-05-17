package io.snice.modem.actors.fsm;

public enum ModemState {
    CONNECTING, CONNECTED, FIRMWARE, RESET, READY, CMD, DISCONNECTING, TERMINATED;
}
