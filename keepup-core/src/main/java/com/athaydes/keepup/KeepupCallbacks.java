package com.athaydes.keepup;

import com.athaydes.keepup.api.KeepupException;
import com.athaydes.keepup.api.UpgradeInstaller;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class KeepupCallbacks {

    final BiFunction<String, File, CompletableFuture<Boolean>> onUpdate;
    final Runnable onNoUpdate;
    final Consumer<KeepupException> onError;
    final Runnable doneWithoutUpdate;
    final Consumer<UpgradeInstaller> doneWithUpdate;

    public KeepupCallbacks(BiFunction<String, File, CompletableFuture<Boolean>> onUpdate,
                           Runnable onNoUpdate,
                           Consumer<KeepupException> onError,
                           Runnable doneWithoutUpdate,
                           Consumer<UpgradeInstaller> doneWithUpdate) {
        this.onUpdate = onUpdate;
        this.onNoUpdate = onNoUpdate;
        this.onError = onError;
        this.doneWithoutUpdate = doneWithoutUpdate;
        this.doneWithUpdate = doneWithUpdate;
    }
}
