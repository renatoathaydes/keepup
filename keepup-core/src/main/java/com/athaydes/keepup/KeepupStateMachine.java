package com.athaydes.keepup;

import com.athaydes.keepup.api.KeepupConfig;
import com.athaydes.keepup.api.KeepupException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.athaydes.keepup.IoUtils.currentApp;
import static com.athaydes.keepup.IoUtils.looksLikeJlinkApp;
import static com.athaydes.keepup.IoUtils.setFilePermissions;
import static com.athaydes.keepup.api.KeepupException.ErrorCode.CANNOT_REMOVE_UPGRADE_ZIP;
import static com.athaydes.keepup.api.KeepupException.ErrorCode.CREATE_UPDATE_SCRIPT;
import static com.athaydes.keepup.api.KeepupException.ErrorCode.CURRENT_NOT_JLINK_APP;
import static com.athaydes.keepup.api.KeepupException.ErrorCode.DONE_CALLBACK;
import static com.athaydes.keepup.api.KeepupException.ErrorCode.DOWNLOAD;
import static com.athaydes.keepup.api.KeepupException.ErrorCode.LATEST_VERSION_CHECK;
import static com.athaydes.keepup.api.KeepupException.ErrorCode.NO_UPDATE_CALLBACK;
import static com.athaydes.keepup.api.KeepupException.ErrorCode.UNPACK;
import static com.athaydes.keepup.api.KeepupException.ErrorCode.UPGRADE_NOT_JLINK_APP;
import static com.athaydes.keepup.api.KeepupException.ErrorCode.VERIFY_UPDATE;

public final class KeepupStateMachine {

    private static final AtomicBoolean isFirstRun = new AtomicBoolean(true);

    private final KeepupConfig config;
    private final KeepupCallbacks callbacks;
    private final KeepupLogger log;

    public KeepupStateMachine(KeepupConfig config, KeepupCallbacks callbacks) {
        this.config = config;
        this.callbacks = callbacks;
        this.log = new KeepupLogger(config.keepupLog());
    }

    public void start() {
        config.executor().submit(() -> {
            if (isFirstRun.getAndSet(false)) {
                log.log("First run");
                File unpackedApp = IoUtils.unpackedApp(config.appHome());
                if (unpackedApp.isDirectory()) {
                    // we have just updated and not cleaned up yet
                    cleanupPreviousUpdate(unpackedApp);
                    noUpdate(callbacks);
                    return;
                }
            }
            var appLocation = currentApp();
            if (looksLikeJlinkApp(appLocation, config)) {
                checkForUpdate(config, callbacks);
            } else {
                endWithError(new KeepupException(CURRENT_NOT_JLINK_APP, "Home: " + appLocation));
            }
        });
    }

    private void checkForUpdate(KeepupConfig config, KeepupCallbacks callbacks) {
        log.log("Checking for update");
        try {
            config.distributor().findLatestVersion()
                    .ifPresentOrElse(
                            v -> updateTo(v, config, callbacks),
                            () -> noUpdate(callbacks));
        } catch (Exception e) {
            endWithError(new KeepupException(LATEST_VERSION_CHECK, e));
        }
    }

    private void cleanupPreviousUpdate(File unpackedApp) {
        log.log("Cleaning up previous update");
        try {
            IoUtils.deleteContents(unpackedApp);
            Files.delete(unpackedApp.toPath());
        } catch (IOException e) {
            // ignore error
            log.log("ERROR: " + e);
        }
    }

    private void noUpdate(KeepupCallbacks callbacks) {
        log.log("No update available");
        try {
            callbacks.onNoUpdate.run();
            endEarly();
        } catch (Exception e) {
            endWithError(new KeepupException(NO_UPDATE_CALLBACK, e));
        }
    }

    private void updateTo(String newVersion,
                          KeepupConfig config,
                          KeepupCallbacks callbacks) {
        config.executor().submit(() -> {
            log.log("Downloading version " + newVersion);
            try {
                var zip = config.distributor().download(newVersion);
                verifyUpdate(newVersion, zip, config, callbacks);
            } catch (Exception e) {
                endWithError(new KeepupException(DOWNLOAD, e));
            }
        });
    }

    private void verifyUpdate(String newVersion,
                              File zip,
                              KeepupConfig config,
                              KeepupCallbacks callbacks) {
        config.executor().submit(() -> {
            log.log("Verifying update");
            try {
                callbacks.onUpdate.apply(newVersion, zip).whenComplete((continueUpdate, error) -> {
                    if (error != null) {
                        endWithError(new KeepupException(VERIFY_UPDATE, error));
                    } else if (continueUpdate) {
                        unpackNewVersion(zip, config, callbacks);
                    } else {
                        log.log("Update rejected");
                        endEarly();
                    }
                });
            } catch (Exception e) {
                endWithError(new KeepupException(VERIFY_UPDATE, e));
            }
        });
    }

    private void unpackNewVersion(File zip,
                                  KeepupConfig config,
                                  KeepupCallbacks callbacks) {
        config.executor().submit(() -> {
            log.log("Unpacking update");
            try {
                var newVersionDir = IoUtils.unpack(zip, config.appHome());
                if (looksLikeJlinkApp(newVersionDir, config)) {
                    setFilePermissions(newVersionDir, config.appName());
                    createInstaller(zip, config, callbacks);
                } else {
                    endWithError(new KeepupException(UPGRADE_NOT_JLINK_APP,
                            "Upgrade location: " + newVersionDir));
                }
            } catch (Exception e) {
                endWithError(new KeepupException(UNPACK, e));
            }
        });
    }

    private void createInstaller(File zip,
                                 KeepupConfig config,
                                 KeepupCallbacks callbacks) {
        config.executor().submit(() -> {
            log.log("Creating installer");
            try {
                var installer = InstallerCreator.create(config);
                if (zip.delete()) {
                    log.log("Upgrade successful");
                    callbacks.onDone.accept(installer);
                } else {
                    endWithError(new KeepupException(CANNOT_REMOVE_UPGRADE_ZIP,
                            "Location: " + zip));
                }
            } catch (Exception e) {
                KeepupException error;
                if (e instanceof KeepupException) {
                    error = (KeepupException) e;
                } else {
                    error = new KeepupException(CREATE_UPDATE_SCRIPT, e);
                }
                endWithError(error);
            }
        });
    }

    private void endEarly() {
        log.log("DONE");
        try {
            callbacks.onDone.accept(null);
        } catch (Exception e) {
            log.log("ERROR: " + e);
            callbacks.onError.accept(new KeepupException(DONE_CALLBACK, e));
        }
    }

    private void endWithError(KeepupException error) {
        log.log("ERROR: " + error);
        try {
            callbacks.onError.accept(error);
        } finally {
            callbacks.onDone.accept(null);
        }
    }
}
