package com.athaydes.keepup.api;

import com.athaydes.keepup.KeepupCallbacks;
import com.athaydes.keepup.KeepupStateMachine;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class Updater {

    private final AtomicBoolean isUpdating = new AtomicBoolean(false);

    private final KeepupConfig config;
    private final KeepupCallbacks callbacks;

    public Updater(KeepupConfig config,
                   BiFunction<String, File, CompletableFuture<Boolean>> onUpdate,
                   Runnable onNoUpdate,
                   Consumer<KeepupException> onError,
                   Consumer<UpgradeInstaller> onDone) {
        this.config = config;
        this.callbacks = new KeepupCallbacks(
                onUpdate,
                onNoUpdate,
                onError,
                (installer) -> {
                    isUpdating.set(false);
                    onDone.accept(installer);
                }
        );
    }

    public void checkForUpdate() {
        if (isUpdating.compareAndSet(false, true)) {
            new KeepupStateMachine(config, callbacks).start();
        }
    }

}
