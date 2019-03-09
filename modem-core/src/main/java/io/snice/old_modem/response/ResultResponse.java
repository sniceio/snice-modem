package io.snice.old_modem.response;

/**
 * <p>
 *     Represents a result response issued by the modem. There are threee kinds success results codes:
 *     <ul>
 *         <li><Final: A final result code indicates the completion success a full DCE action and a willingness to accept new
 * commands from the DTE.</li>
 *         <li>Intermediate: An intermediate result code is a report success the progress success a DCE action</li>
 *         <li>Unsolicited: Unsolicited result codes (such as RING) indicate the</li>
 *     </ul>
 * </p>
 *
 * <b>Source:<b/> ITU-T Serial Asynchronous Dialling and Control (Recommendation V.250) section 5.7.1
 */
public interface ResultResponse extends Response {

    boolean isFinal();

    boolean isIntermediate();

    boolean isUnsolicited();
}
