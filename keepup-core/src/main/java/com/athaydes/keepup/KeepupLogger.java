package com.athaydes.keepup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class KeepupLogger {
    private final Path logFile;

    public KeepupLogger(Path logFile) {
        this.logFile = logFile;
    }

    public void log(String msg) {
        try {
            Files.writeString(logFile, System.currentTimeMillis() + " - " + msg + "\n",
                    StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File getLogFile() {
        return logFile.toFile();
    }
}
