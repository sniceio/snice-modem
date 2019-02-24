package io.snice.modem.actors.fsm;

import io.snice.buffer.Buffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * See table 1/V.250 - Result Codes
 *
 * <b>Source:<b/> ITU-T Serial Asynchronous Dialling and Control (Recommendation V.250) section 5.7.1
 */
public class ItuResultCodes {

    public static List<ItuResultCodes> initialize(final boolean verbose, final char S3, final char S4) {
        final List<ItuResultCodes> results = new ArrayList<>(Code.values().length);

        Arrays.stream(Code.values()).forEach(rc -> {
            final byte[] code = rc.getVerboseCode().getBytes(); // TODO: encoding

            final byte[] encoded = new byte[code.length + 4];
            System.arraycopy(code, 0, encoded, 2, code.length);

            encoded[0] = (byte)S3; // not really correct
            encoded[1] = (byte)S4;
            encoded[encoded.length - 1] = (byte)S4;
            encoded[encoded.length - 2] = (byte)S3;
            results.add(new ItuResultCodes(rc, encoded));
        });

        return Collections.unmodifiableList(results);
    }

    private final Code code;
    private final byte[] encoded;

    private ItuResultCodes(final Code code, final byte[] encoded) {
        this.code = code;
        this.encoded = encoded;
    }

    public String toString() {
        return code.toString();
    }

    public Code getCode() {
        return code;
    }

    public boolean match(final Buffer buffer) {
        return buffer.endsWith(encoded);
    }

    public enum Code {
        OK(0, "OK", true),
        CONNECT(1, "CONNECT", false),
        RING(2, "RING", false),
        NO_CARRIER(3, "NO CARRIER", true),
        ERROR(4, "ERROR", true),
        NO_DIALTONE(6, "NO DIALTONE", true),
        BUSY(7, "BUSY", true),
        NO_ANSWER(8, "NO ANSWER", true);

        private final int code;
        private final String verboseCode;
        private final boolean isFinal;

        Code(final int code, final String verboseCode, final boolean isFinal) {
            this.code = code;
            this.verboseCode = verboseCode;
            this.isFinal = isFinal;
        }

        public int getCode() {
            return code;
        }

        public String getVerboseCode() {
            return verboseCode;
        }

        public boolean isFinal() {
            return isFinal;
        }

        @Override
        public String toString() {
            return verboseCode;
        }

    }
}
