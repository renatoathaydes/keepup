package com.athaydes.keepup;

import com.athaydes.keepup.api.KeepupException;

import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class KeepupCallbacks {
    final BiConsumer<String, File> onUpdate;
    final Runnable onNoUpdate;
    final Consumer<KeepupException> onError;
    final Runnable onDone;

    public KeepupCallbacks(BiConsumer<String, File> onUpdate,
                           Runnable onNoUpdate,
                           Consumer<KeepupException> onError,
                           Runnable onDone) {
        this.onUpdate = onUpdate;
        this.onNoUpdate = onNoUpdate;
        this.onError = onError;
        this.onDone = onDone;
    }
}
