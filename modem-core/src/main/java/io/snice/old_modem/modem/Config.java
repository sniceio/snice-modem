package io.snice.old_modem.modem;

public class Config {

    /**
     * <p>
     * Command line termination character.
     * </p>
     *
     * <p>
     * This S-parameter represents the decimal IA5 value of the character recognized by the DCE from the
     * DTE to terminate an incoming command line. It is also generated by the DCE as part of the header,
     * trailer, and terminator for result codes and information text, along with the S4 parameter (see the
     * description of the V parameter for usage).
     * </p>
     *
     * Source: ITU-T Serial Asynchronous Dialling and Control (Recommendation V.250) section 6.2.1
     *
     * @return
     */
    public char getS3() {
        return '\r';
    }

    /**
     * <p>
     * Response formatting character
     * </p>
     *
     * <p>
     * This S-parameter represents the decimal IA5 value of the character generated by the DCE as part of
     * the header, trailer, and terminator for result codes and information text, along with the S3 parameter
     * (see the description of the V parameter for usage). If the value of S4 is changed in a command line,
     * the result code issued in response to that command line will use the new value of S4
     * </p>
     *
     * Source: ITU-T Serial Asynchronous Dialling and Control (Recommendation V.250) section 6.2.2
     *
     * @return
     */
    public char getS4() {
        return '\n';
    }

    /**
     * <p>
     * DCE response format
     * </p>
     *
     * <p>
     * The setting of this parameter determines the contents of the header and trailer transmitted with
     * result codes and information responses. It also determines whether result codes are transmitted in a
     * numeric form or an alphabetic (or "verbose") form. The text portion of information responses is not
     * affected by this setting.
     * </p>
     *
     * Source: ITU-T Serial Asynchronous Dialling and Control (Recommendation V.250) section 6.2.6
     *
     * @return
     */
    public boolean isVerboseResponseFormat() {
        return true;
    }

    /**
     * <p>
     * Command echo
     * </p>
     *
     * <p>
     * The setting of this parameter determines whether or not the DCE echoes characters received from
     * the DTE during command state and online command state (see 5.2.3).
     * </p>
     *
     * Source: ITU-T Serial Asynchronous Dialling and Control (Recommendation V.250) section 6.2.4
     *
     * @return
     */
    public boolean isCommandEchoOn() {
        return true;
    }
}
