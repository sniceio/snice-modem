package io.snice.old_modem.command;

import io.snice.old_modem.Command;

abstract class BaseCommand implements Command {

    private final String cmd;
    private final byte[] cmdAsBytes;

    protected BaseCommand(String cmd) {
        this.cmd = cmd;
        cmdAsBytes = (cmd + "\r\n").getBytes();
    }

    @Override
    public String getAtCmd() {
        return cmd;
    }

    @Override
    public byte[] convert() {
        return cmdAsBytes;
    }
}
