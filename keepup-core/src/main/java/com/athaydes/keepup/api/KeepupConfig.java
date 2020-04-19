package com.athaydes.keepup.api;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Configuration to be used by {@link Keepup}.
 * <p>
 * All methods of this class are called only a single time, and the returned value is remembered by
 * Keepup. This makes it simpler to implement it without having to keep internal state.
 */
public interface KeepupConfig {
    /**
     * @return the app name, which should match the name of the application launcher, and,
     * usually, the top-level directory inside an upgrade zip file.
     */
    String appName();

    /**
     * @return location where the application can write internal data which is not part of the installation.
     * By default, Keepup uses the {@code user.home} System Property followed by {@code "/." + appName()}.
     * Notice that this is NOT the installation folder.
     */
    default File appHome() {
        return new File(System.getProperty("user.home"), "." + appName());
    }

    /**
     * @return an {@link AppDistributor} for obtaining application upgrades.
     */
    AppDistributor distributor();

    /**
     * @return an {@link ExecutorService} that will be used to run all {@link Keepup} callbacks.
     */
    default ExecutorService executor() {
        return Executors.newSingleThreadExecutor();
    }

    /**
     * @return A simple {@link Consumer} that will be called by {@link Keepup} for logging.
     * By default, this logger will write to a file at {@link KeepupConfig#appHome()} called
     * {@code keepup.log}.
     */
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
