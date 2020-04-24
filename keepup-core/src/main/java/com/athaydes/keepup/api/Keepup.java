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

    /**
     * A callback that does not do anything.
     */
    public static final Runnable NO_OP = () -> {
    };

    private final KeepupConfig config;

    private volatile BiFunction<String, File, CompletableFuture<Boolean>> onUpdate;
    private volatile Runnable onNoUpdate;
    private volatile Consumer<KeepupException> onError;
    private volatile Runnable doneWithoutUpdate;
    private volatile Consumer<UpgradeInstaller> doneWithUpdate;

    public Keepup(KeepupConfig config) {
        this.config = new KeepupConfigWrapper(config);
        onUpdate = (v, f) -> CompletableFuture.completedFuture(true);
        onNoUpdate = NO_OP;
        onError = Throwable::printStackTrace;
        doneWithoutUpdate = this::close;
        doneWithUpdate = (installer) -> {
            installer.installUpgradeOnExit();
            close();
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
     * This is similar to a {@code finally} block in a Java try-catch statement, as one of the given
     * callbacks will be called even when there is an error, or there is no update, or the update is aborted for
     * any reason.
     * <p>
     * By default, the {@code doneWithUpdate} callback calls {@link UpgradeInstaller#installUpgradeOnExit()} and
     * then closes this instance of {@link Keepup}. The {@code doneWithoutUpdate} callback only closes this instance
     * of {@link Keepup}.
     *
     * @param doneWithoutUpdate callback called when an update check ended without an update, for whatever reason
     * @param doneWithUpdate    callback called when an update check results in a successful update.
     * @return this
     */
    public Keepup onDone(Runnable doneWithoutUpdate,
                         Consumer<UpgradeInstaller> doneWithUpdate) {
        this.doneWithoutUpdate = Objects.requireNonNull(doneWithoutUpdate);
        this.doneWithUpdate = Objects.requireNonNull(doneWithUpdate);
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
        return new Updater(config, onUpdate, onNoUpdate, onError, doneWithoutUpdate, doneWithUpdate);
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
