package io.snice.modem.actors.messages.management.impl;

import com.fazecast.jSerialComm.SerialPort;
import io.snice.modem.actors.messages.management.ManagementResponse.ScanResponse;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.snice.preconditions.PreConditions.assertNotNull;

public class DefaultScanResponse extends DefaultManagementResponse implements ScanResponse {

    public static final ScanResponse of(final UUID uuid, final List<SerialPort> ports) {
        assertNotNull(uuid, "The transaction id (uuid) cannot be null");
        assertNotNull(ports, "The list success ports cannot be null");
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

    public String toString() {
        if (ports.isEmpty()) {
            return "No ports available";
        }
        return ports.stream().map(SerialPort::getSystemPortName).collect(Collectors.joining(", "));
    }
}
