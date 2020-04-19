package com.athaydes.keepup.api;

import java.io.Closeable;
import java.io.File;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;

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
            installer.installUpgradeOnExit();
            config.executor().shutdown();
        };

        // app home must exist to avoid errors later
        config.appHome().mkdirs();
    }

    public KeepupConfig getConfig() {
        return config;
    }

    public Keepup onUpdate(BiFunction<String, File, CompletableFuture<Boolean>> onUpdate) {
        this.onUpdate = Objects.requireNonNull(onUpdate);
        return this;
    }

    public Keepup onNoUpdateRequired(Runnable onNoUpdate) {
        this.onNoUpdate = Objects.requireNonNull(onNoUpdate);
        return this;
    }

    public Keepup onError(Consumer<KeepupException> onError) {
        this.onError = Objects.requireNonNull(onError);
        return this;
    }

    public Keepup onDone(Consumer<UpgradeInstaller> runnable) {
        this.onDone = Objects.requireNonNull(runnable);
        return this;
    }

    public Updater createUpdater() {
        return new Updater(config, onUpdate, onNoUpdate, onError, onDone);
    }

    @Override
    public void close() {
        config.executor().shutdown();
    }
}
