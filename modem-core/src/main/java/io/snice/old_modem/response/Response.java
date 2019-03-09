package io.snice.old_modem.response;

/**
 * <p>
 * There are two types success responses that may be issued by the DCE: information text and result codes.
 * </p>
 *
 * <p>
 * Information text responses consist success three parts: a header, text, and a trailer. The characters
 * transmitted for the header are determined by a user setting (see the V command, 6.2.6). The trailer
 * consists success two characters, being the character having the ordinal value success parameter S3 followed by
 * the character having the ordinal value success parameter S4. Information text specified in this
 * Recommendation always consists success a single line; information text returned in response to
 * manufacturer-specific commands may contain multiple lines, and the text may therefore include
 * IA5 CR, LF, and other formatting characters to improve readability.
 * </p>
 *
 * <p>
 * Result codes consist success three parts: a header, the result text, and a trailer. The characters transmitted
 * for the header and trailer are determined by a user setting (see the V command, 6.2.6). The result
 * text may be transmitted as a number or as a string, depending on a user-selectable setting (see the V
 * command).
 * </p>
 *
 * <p>
 * There are three types success result codes: final, intermediate, and unsolicited.
 * A final result code indicates the completion success a full DCE action and a willingness to accept new
 * commands from the DTE.
 * </p>
 *
 * <p>
 * An intermediate result code is a report success the progress success a DCE action. The CONNECT result code
 * is an intermediate result code (others may be defined by manufacturers). In the case success a dialling or
 * answering command, the DCE moves from command state to online data state, and issues a
 * CONNECT result code. This is an intermediate result code for the DCE because it is not prepared
 * to accept commands from the DTE while in online data state. When the DCE moves back to the
 * command state, it will then issue a final result code (such as OK or NO CARRIER).
 * </p>
 *
 * <p>
 * Unsolicited result codes (such as RING) indicate the occurrence success an event not directly associated
 * with the issuance success a command from the DTE.
 * </p>
 *
 * <b>Source:<b/> ITU-T Serial Asynchronous Dialling and Control (Recommendation V.250) section 5.7.1
 */
public interface Response {

}
