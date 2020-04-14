package com.athaydes.keepup;

import com.athaydes.keepup.api.KeepupConfig;
import com.athaydes.keepup.api.KeepupException;

import java.io.File;
import java.io.IOException;

import static com.athaydes.keepup.api.KeepupException.ErrorCode.DOWNLOAD;
import static com.athaydes.keepup.api.KeepupException.ErrorCode.LATEST_VERSION_CHECK;
import static com.athaydes.keepup.api.KeepupException.ErrorCode.UPDATE;

public final class KeepupRunner {

    public static void run(KeepupConfig config,
                           KeepupCallbacks keepupCallbacks) {
        config.executor().submit(() -> {
            File unpackedApp = Unpacker.unpackedApp(config.appHome());
            if (unpackedApp.isDirectory()) {
                // we have just updated and not cleaned up yet
                cleanupPreviousUpdate(keepupCallbacks, unpackedApp);
            } else {
                initiateUpdate(config, keepupCallbacks);
            }
        });
    }

    private static void initiateUpdate(KeepupConfig config, KeepupCallbacks keepupCallbacks) {
        try {
            config.distributor().findLatestVersion()
                    .ifPresentOrElse(
                            v -> updateTo(v, config, keepupCallbacks),
                            () -> noUpdate(keepupCallbacks));
        } catch (Exception e) {
            try {
                keepupCallbacks.onError.accept(new KeepupException(LATEST_VERSION_CHECK, e));
            } finally {
                keepupCallbacks.onDone.run();
            }
        }
    }

    private static void cleanupPreviousUpdate(KeepupCallbacks keepupCallbacks, File unpackedApp) {
        try {
            keepupCallbacks.onNoUpdate.run();
        } finally {
            try {
                Unpacker.deleteContents(unpackedApp);
                unpackedApp.delete();
            } catch (IOException e) {
                // ignore error, try to cleanup again next time
                e.printStackTrace();
            }
            keepupCallbacks.onDone.run();
        }
    }

    private static void noUpdate(KeepupCallbacks keepupCallbacks) {
        try {
            keepupCallbacks.onNoUpdate.run();
        } finally {
            keepupCallbacks.onDone.run();
        }
    }

    private static void updateTo(String newVersion,
                                 KeepupConfig config,
                                 KeepupCallbacks keepupCallbacks) {
        config.executor().submit(() -> {
            try {
                var zip = config.distributor().download(newVersion);
                doUpdate(newVersion, zip, config, keepupCallbacks);
            } catch (Exception e) {
                try {
                    keepupCallbacks.onError.accept(new KeepupException(DOWNLOAD, e));
                } finally {
                    keepupCallbacks.onDone.run();
                }
            }
        });
    }

    private static void doUpdate(String newVersion, File zip,
                                 KeepupConfig config, KeepupCallbacks keepupCallbacks) {
        config.executor().submit(() -> {
            try {
                keepupCallbacks.onUpdate.accept(newVersion, zip);
            } catch (Exception e) {
                keepupCallbacks.onError.accept(new KeepupException(UPDATE, e));
            } finally {
                keepupCallbacks.onDone.run();
            }
        });
    }
}
