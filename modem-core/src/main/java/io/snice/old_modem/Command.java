package io.snice.old_modem;

public interface Command {

    /**
     * Convert the command to a byte stream that we can write to the port
     *
     * @return
     */
    byte[] convert();


    String getAtCmd();

}
