package io.snice.modem.actors.messages.management.impl;

import com.fazecast.jSerialComm.SerialPort;
import io.snice.modem.actors.messages.management.ManagementRequest.ScanRequest;
import io.snice.modem.actors.messages.management.ManagementResponse.ScanResponse;

import java.util.List;

public class DefaultScanRequest extends DefaultManagementRequest implements ScanRequest {


    public static ScanRequest of() {
        return new DefaultScanRequest();
    }

    private DefaultScanRequest() {
        // left empty intentionally
    }

    @Override
    public ScanResponse createResult(final List<SerialPort> ports) {
        return DefaultScanResponse.of(getTransactionId(), ports);
    }
}
