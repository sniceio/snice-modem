package io.snice.modem.actors.messages.management.impl;

import io.snice.modem.actors.messages.management.ManagementResponse.ConnectResponse;

import java.util.UUID;

public class ErrorConnectResponse extends DefaultManagementResponse implements ConnectResponse {

    private final String msg;

    protected ErrorConnectResponse(final UUID uuid, final String msg) {
        super(uuid);
        this.msg = msg;
    }

    public String getError() {
        return msg;
    }

    @Override
    public boolean isFailure() {
        return true;
    }

}
