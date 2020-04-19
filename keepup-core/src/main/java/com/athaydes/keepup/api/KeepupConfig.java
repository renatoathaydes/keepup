package com.athaydes.keepup.api;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public interface KeepupConfig {
    String appName();

    default File appHome() {
        return new File(System.getProperty("user.home"), "." + appName());
    }

    AppDistributor distributor();

    default ExecutorService executor() {
        return Executors.newSingleThreadExecutor();
    }

    default Consumer<String> logger() {
        var logFile = new File(appHome(), "keepup.log").toPath();
        return (msg) -> {
            try {
                Files.writeString(logFile, System.currentTimeMillis() + " - " + msg + "\n",
                        StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }
}
