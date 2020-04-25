package com.athaydes.keepup.api;

import java.util.function.Consumer;

/**
 * The result of a successful upgrade cycle.
 * <p>
 * When an upgrade cycle ends in success, the {@link Keepup#onDone(Runnable, Consumer)} second callback will be called
 * with an instance of this class, so that the application can decide when to finalize the application upgrade.
 * <p>
 * Only one of the methods of this class should be called, as each method uses a different strategy for performing
 * the upgrade.
 */
public interface UpgradeInstaller {
    /**
     * Installs and launches the upgraded application without killing the current process.
     * <p>
     * Notice that if the application code does not terminate the application by itself, the user will
     * have two applications running at the same time: the old, and the upgrade that has just been installed.
     * <p>
     * This is usually a mistake, and you should prefer to use the other methods of this class instead of this one.
     */
    void launchUpgradedAppWithoutExiting();

    /**
     * Invoke the installer in a separate process to install and launch the upgraded application,
     * then exit this process.
     * <p>
     * This method never returns normally.
     */
    void quitAndLaunchUpgradedApp();

    /**
     * Installs the downloaded upgrade once this process exits.
     * <p>
     * This is currently done by adding a shutdown hook to the JVM, which means that if the JVM dies
     * abnormally due to a hard crash, the installer may never run.
     */
    void installUpgradeOnExit();
}
