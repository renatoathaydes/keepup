package com.athaydes.keepup.api;

import java.io.Closeable;
import java.io.File;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * This class is the center of the Keepup library.
 * <p>
 * It is used to configure and define the custom callback behaviour of the upgrading process.
 * <p>
 * As it utilizes an {@link java.util.concurrent.ExecutorService} (provided by {@link KeepupConfig})
 * to run all callbacks in the background, it must be closed when it is no longer required.
 */
public final class Keepup implements Closeable, AutoCloseable {

    private final KeepupConfig config;

    private volatile BiFunction<String, File, CompletableFuture<Boolean>> onUpdate;
    private volatile Runnable onNoUpdate;
    private volatile Consumer<KeepupException> onError;
    private volatile Consumer<UpgradeInstaller> onDone;

    public Keepup(KeepupConfig config) {
        this.config = new KeepupConfigWrapper(config);
        onUpdate = (v, f) -> CompletableFuture.completedFuture(true);
        onNoUpdate = () -> {
        };
        onError = Throwable::printStackTrace;
        onDone = (installer) -> {
            if (installer != null) {
                installer.installUpgradeOnExit();
            }
            config.executor().shutdown();
        };

        // app home must exist to avoid errors later
        config.appHome().mkdirs();
    }

    public KeepupConfig getConfig() {
        return config;
    }

    /**
     * Define what to do when an update has just been downloaded.
     * <p>
     * By default, this callback simple returns true and the update continues.
     *
     * @param onUpdate callback
     * @return this
     */
    public Keepup onUpdate(BiFunction<String, File, CompletableFuture<Boolean>> onUpdate) {
        this.onUpdate = Objects.requireNonNull(onUpdate);
        return this;
    }

    /**
     * Define what to do when there is no update available.
     * <p>
     * By default, this callback does not do anything.
     *
     * @param onNoUpdate callback
     * @return this
     */
    public Keepup onNoUpdateRequired(Runnable onNoUpdate) {
        this.onNoUpdate = Objects.requireNonNull(onNoUpdate);
        return this;
    }

    /**
     * Define what to do when an error occurs.
     * <p>
     * An error can occur both when another callback is run, or when something else fails during an upgrade.
     * <p>
     * By default, this callback logs the stacktrace to stderr.
     *
     * @param onError callback
     * @return this
     */
    public Keepup onError(Consumer<KeepupException> onError) {
        this.onError = Objects.requireNonNull(onError);
        return this;
    }

    /**
     * Define what to do when an update check is done, whether successfully or not.
     * <p>
     * This is similar to a {@code finally} block in a Java try-catch statement, as it will be called
     * even when there is an error, or there is no update, or the update is aborted for any reason.
     * <p>
     * By default, this callback calls {@link UpgradeInstaller#installUpgradeOnExit()} if the
     * upgrade was successful, then, regardless of whether or not the upgrade succeeded,
     * closes this instance of {@link Keepup}.
     *
     * <b>The callback will receive {@code null} if the upgrade did not proceed successfully all the way to the end.</b>
     * In other words, it will only receive a non-null {@link UpgradeInstaller} if the upgrade has been performed
     * successfully.
     *
     * @param onDone callback
     * @return this
     */
    public Keepup onDone(Consumer<UpgradeInstaller> onDone) {
        this.onDone = Objects.requireNonNull(onDone);
        return this;
    }

    /**
     * Create an instance of {@link Updater} for future update checks.
     * <p>
     * This method must be called only after all callbacks have been setup.
     *
     * @return an {@link Updater} that can initiate checks for new releases.
     */
    public Updater createUpdater() {
        return new Updater(config, onUpdate, onNoUpdate, onError, onDone);
    }

    /**
     * Close this {@link Keepup} instance.
     * <p>
     * Calling this will close the {@link java.util.concurrent.ExecutorService} provided by the
     * associated {@link KeepupConfig}.
     */
    @Override
    public void close() {
        config.executor().shutdown();
    }
}
