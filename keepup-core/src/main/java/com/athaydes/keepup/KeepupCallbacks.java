package com.athaydes.keepup;

import com.athaydes.keepup.api.UpgradeInstaller;
import com.athaydes.keepup.api.KeepupException;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class KeepupCallbacks {

    final BiFunction<String, File, CompletableFuture<Boolean>> onUpdate;
    final Runnable onNoUpdate;
    final Consumer<KeepupException> onError;
    final Consumer<UpgradeInstaller> onDone;

    public KeepupCallbacks(BiFunction<String, File, CompletableFuture<Boolean>> onUpdate,
                           Runnable onNoUpdate,
                           Consumer<KeepupException> onError,
                           Consumer<UpgradeInstaller> onDone) {
        this.onUpdate = onUpdate;
        this.onNoUpdate = onNoUpdate;
        this.onError = onError;
        this.onDone = onDone;
    }
}
