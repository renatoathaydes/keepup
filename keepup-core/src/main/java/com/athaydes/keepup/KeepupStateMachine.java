package com.athaydes.keepup;

import com.athaydes.keepup.api.AppDistributor;
import com.athaydes.keepup.api.AppVersion;
import com.athaydes.keepup.api.KeepupConfig;
import com.athaydes.keepup.api.KeepupException;
import com.athaydes.keepup.api.UpgradeInstaller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Callable;
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
                    noUpdate();
                    return;
                }
            }
            var appLocation = currentApp();
            if (looksLikeJlinkApp(appLocation, config.appName())) {
                checkForUpdate();
            } else {
                endWithError(new KeepupException(CURRENT_NOT_JLINK_APP, "Home: " + appLocation));
            }
        });
    }

    private void checkForUpdate() {
        config.executor().submit(() -> {
            log.log("Checking for update");
            try {
                findVersion(config.distributor());
            } catch (Exception e) {
                endWithError(new KeepupException(LATEST_VERSION_CHECK, e));
            }
        });
    }

    private <V extends AppVersion> void findVersion(
            AppDistributor<V> distributor) throws Exception {
        distributor.findLatestVersion().ifPresentOrElse(v -> {
            invokeDownload(v.name(), () -> distributor.download(v));
        }, this::noUpdate);
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

    private void noUpdate() {
        log.log("No update available");
        try {
            callbacks.onNoUpdate.run();
            endEarly();
        } catch (Exception e) {
            endWithError(new KeepupException(NO_UPDATE_CALLBACK, e));
        }
    }

    private void invokeDownload(String newVersion, Callable<File> download) {
        config.executor().submit(() -> {
            log.log("Downloading version " + newVersion);
            try {
                var zip = download.call();
                verifyUpdate(newVersion, zip);
            } catch (Exception e) {
                endWithError(new KeepupException(DOWNLOAD, e));
            }
        });
    }

    private void verifyUpdate(String newVersion, File zip) {
        config.executor().submit(() -> {
            log.log("Verifying update");
            try {
                callbacks.onUpdate.apply(newVersion, zip).whenComplete((continueUpdate, error) -> {
                    if (error != null) {
                        endWithError(new KeepupException(VERIFY_UPDATE, error));
                    } else if (continueUpdate) {
                        unpackNewVersion(zip);
                    } else {
                        log.log("Update rejected");
                        zip.delete();
                        endEarly();
                    }
                });
            } catch (Exception e) {
                endWithError(new KeepupException(VERIFY_UPDATE, e));
            }
        });
    }

    private void unpackNewVersion(File zip) {
        config.executor().submit(() -> {
            log.log("Unpacking update");
            try {
                var newVersionDir = IoUtils.unpack(zip, config.appHome());
                if (looksLikeJlinkApp(newVersionDir, config.appName())) {
                    setFilePermissions(newVersionDir, config.appName());
                    createInstaller(zip);
                } else {
                    endWithError(new KeepupException(UPGRADE_NOT_JLINK_APP,
                            "Upgrade location: " + newVersionDir));
                }
            } catch (Exception e) {
                endWithError(new KeepupException(UNPACK, e));
            }
        });
    }

    private void createInstaller(File zip) {
        config.executor().submit(() -> {
            log.log("Creating installer");
            try {
                var installer = InstallerCreator.create(config);
                if (zip.delete()) {
                    log.log("Upgrade successful");
                    success(installer);
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
        try {
            callbacks.doneWithoutUpdate.run();
        } catch (Exception e) {
            log.log("ERROR: " + e);
            callbacks.onError.accept(new KeepupException(DONE_CALLBACK, e));
        } finally {
            log.log("DONE");
        }
    }

    private void endWithError(KeepupException error) {
        log.log("ERROR: " + error);
        try {
            callbacks.onError.accept(error);
        } finally {
            endEarly();
        }
    }

    private void success(UpgradeInstaller installer) {
        try {
            callbacks.doneWithUpdate.accept(installer);
        } catch (Exception e) {
            log.log("ERROR: " + e);
            callbacks.onError.accept(new KeepupException(DONE_CALLBACK, e));
        } finally {
            log.log("DONE");
        }
    }
}
