package com.athaydes.keepup.api;

import com.athaydes.keepup.KeepupCallbacks;
import com.athaydes.keepup.KeepupRunner;
import com.athaydes.keepup.LauncherCreator;
import com.athaydes.keepup.Unpacker;

import java.io.Closeable;
import java.io.File;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.athaydes.keepup.LauncherCreator.updateLauncher;
import static com.athaydes.keepup.api.KeepupException.ErrorCode.UNPACK;

public final class Keepup implements Closeable, AutoCloseable {

    private final KeepupConfig config;

    private BiConsumer<String, File> onUpdate;
    private Runnable onNoUpdate;
    private Consumer<KeepupException> onError;
    private Runnable onDone;

    public Keepup(KeepupConfig config) {
        this.config = new KeepupConfigWrapper(config);
        onUpdate = (v, f) -> unpack(f).thenRun(this::restartApplication);
        onNoUpdate = () -> {
        };
        onError = Throwable::printStackTrace;
        onDone = () -> {
            config.executor().shutdown();
        };
    }

    public Keepup onUpdate(BiConsumer<String, File> onUpdate) {
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

    public CompletionStage<Void> unpack(File zipFile) {
        var future = new CompletableFuture<Void>();
        config.executor().submit(() -> {
            try {
                Unpacker.unpack(zipFile, config.appHome());
                LauncherCreator.create(config);
                future.complete(null);
            } catch (Exception e) {
                onError.accept(new KeepupException(UNPACK, e));
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public void restartApplication() {
        var updateLauncher = new File(config.appHome(), updateLauncher(config));
        try {
            var proc = new ProcessBuilder(updateLauncher.getAbsolutePath())
                    .inheritIO()
                    .start();
            var code = proc.waitFor();
            System.exit(code);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onDone(Runnable runnable) {
        this.onDone = Objects.requireNonNull(runnable);
    }

    public void checkForUpdate() {
        KeepupRunner.run(config,
                new KeepupCallbacks(onUpdate, onNoUpdate, onError, onDone));
    }

    @Override
    public void close() {
        config.executor().shutdown();
    }
}
