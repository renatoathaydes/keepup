package com.athaydes.keepup.api;

import java.util.function.Consumer;

/**
 * The result of a successful update cycle.
 * <p>
 * When an update cycle ends in success, the {@link Keepup#onDone(Runnable, Consumer)} second callback will be called
 * with an instance of this class, so that the application can decide when to finalize the application update.
 * <p>
 * Only one of the methods of this class should be called, as each method uses a different strategy for performing
 * the update.
 */
public interface UpdateInstaller {
    /**
     * Installs and launches the updated application without killing the current process.
     * <p>
     * Notice that if the application code does not terminate the application by itself, the user will either
     * have two applications running at the same time (the old, and the update that has just been installed)
     * or the installer will fail after some time on Operating Systems (Windows) that do not allow the deletion
     * of the old installation while the application is still running.
     * <p>
     * This may result in the update failing, so you should prefer to use the other methods of
     * this class instead of this one.
     */
    void launchUpdatedAppWithoutExiting();

    /**
     * Invoke the installer in a separate process to install and launch the updated application,
     * then exit this process.
     * <p>
     * This method never returns normally.
     */
    void quitAndLaunchUpdatedApp();

    /**
     * Installs the downloaded update once this process exits.
     * <p>
     * This is currently done by adding a shutdown hook to the JVM, which means that if the JVM dies
     * abnormally due to a hard crash, the installer may never run.
     */
    void installUpdateOnExit();
}
