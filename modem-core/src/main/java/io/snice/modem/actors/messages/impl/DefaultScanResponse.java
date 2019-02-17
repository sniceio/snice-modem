package io.snice.modem.actors.messages.impl;

import com.fazecast.jSerialComm.SerialPort;
import io.snice.modem.actors.messages.ManagementResponse.ScanResponse;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static io.snice.preconditions.PreConditions.assertNotNull;

public class DefaultScanResponse extends DefaultManagementResponse implements ScanResponse {

    public static final ScanResponse of(final UUID uuid, final List<SerialPort> ports) {
        assertNotNull(uuid, "The transaction id (uuid) cannot be null");
        assertNotNull(ports, "The list of ports cannot be null");
        return new DefaultScanResponse(uuid, ports);
    }

    private final List<SerialPort> ports;

    private DefaultScanResponse(final UUID uuid, final List<SerialPort> ports) {
        super(uuid);
        this.ports = Collections.unmodifiableList(ports);
    }
    @Override
    public List<SerialPort> getPorts() {
        return ports;
    }
}
