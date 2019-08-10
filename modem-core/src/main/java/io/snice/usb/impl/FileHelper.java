package io.snice.usb.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileHelper {


    public static String getValue(final Path folder, final String file) throws IOException {
        return Files.readString(folder.resolve(file)).stripLeading().stripTrailing();
    }
}
