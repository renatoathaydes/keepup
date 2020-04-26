package com.athaydes.keepup.api;

import com.athaydes.keepup.KeepupCallbacks;
import com.athaydes.keepup.KeepupStateMachine;

import java.io.File;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Simple class that can be used for checking for new updates, i.e. starting a new Keepup update cycle.
 */
public final class Updater {

    private final AtomicBoolean isUpdating = new AtomicBoolean(false);

    private final KeepupConfig config;
    private final KeepupCallbacks callbacks;

    public Updater(KeepupConfig config,
                   BiFunction<String, File, CompletionStage<Boolean>> onUpdate,
                   Runnable onNoUpdate,
                   Consumer<KeepupException> onError,
                   Runnable doneWithoutUpdate,
                   Consumer<UpdateInstaller> doneWithUpdate) {
        this.config = config;
        this.callbacks = new KeepupCallbacks(
                onUpdate,
                onNoUpdate,
                onError,
                () -> {
                    isUpdating.set(false);
                    doneWithoutUpdate.run();
                },
                (installer) -> {
                    isUpdating.set(false);
                    doneWithUpdate.accept(installer);
                }
        );
    }

    /**
     * Check for updates.
     * <p>
     * Calling this method has the effect of starting a Keepup update cycle if no previous cycle is still running.
     * <p>
     * If a previous cycle has not completed when this method is called, then this call has no effect.
     *
     * @see Keepup
     */
    public void checkForUpdate() {
        if (isUpdating.compareAndSet(false, true)) {
            new KeepupStateMachine(config, callbacks).start();
        }
    }

}
