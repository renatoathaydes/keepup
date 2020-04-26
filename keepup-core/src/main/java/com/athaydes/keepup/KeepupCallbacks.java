package com.athaydes.keepup;

import com.athaydes.keepup.api.KeepupException;
import com.athaydes.keepup.api.UpdateInstaller;

import java.io.File;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class KeepupCallbacks {

    final BiFunction<String, File, CompletionStage<Boolean>> onUpdate;
    final Runnable onNoUpdate;
    final Consumer<KeepupException> onError;
    final Runnable doneWithoutUpdate;
    final Consumer<UpdateInstaller> doneWithUpdate;

    public KeepupCallbacks(BiFunction<String, File, CompletionStage<Boolean>> onUpdate,
                           Runnable onNoUpdate,
                           Consumer<KeepupException> onError,
                           Runnable doneWithoutUpdate,
                           Consumer<UpdateInstaller> doneWithUpdate) {
        this.onUpdate = onUpdate;
        this.onNoUpdate = onNoUpdate;
        this.onError = onError;
        this.doneWithoutUpdate = doneWithoutUpdate;
        this.doneWithUpdate = doneWithUpdate;
    }
}
