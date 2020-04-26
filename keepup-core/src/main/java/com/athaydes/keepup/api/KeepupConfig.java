package com.athaydes.keepup.api;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configuration to be used by {@link Keepup}.
 * <p>
 * All methods of this class are called only a single time, and the returned value is remembered by
 * Keepup. This makes it simpler to implement it without having to keep internal state.
 */
public interface KeepupConfig {
    /**
     * @return the app name, which should match the name of the application launcher, and,
     * usually, the top-level directory inside an update zip file.
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
     * @return an {@link AppDistributor} for obtaining application updates.
     */
    AppDistributor<?> distributor();

    /**
     * @return an {@link ExecutorService} that will be used to run all {@link Keepup} callbacks.
     */
    default ExecutorService executor() {
        return Executors.newSingleThreadExecutor();
    }

    /**
     * @return the location of Keepup's log file.
     * By default, this is a file at {@link KeepupConfig#appHome()} called
     * {@code keepup.log}.
     */
    default Path keepupLog() {
        return appHome().toPath().resolve("keepup.log");
    }
}
