package com.athaydes.keepup;

import com.athaydes.keepup.api.InstallerArgs;

import java.io.File;
import java.io.IOException;

final class AppInstaller {

    public static void main(String[] args) {
        InstallerArgs installerArgs;
        try {
            installerArgs = InstallerArgs.of(args);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        var log = new KeepupLogger(installerArgs.getKeepupLog());

        try {
            run(installerArgs, log);
            log.log("AppInstaller successful");
        } catch (Exception e) {
            log.log("ERROR: AppInstaller - " + e);
        }
    }

    private static void run(InstallerArgs installerArgs, KeepupLogger log) throws Exception {
        var currVersion = installerArgs.getCurrentVersion().toFile();
        var newVersion = installerArgs.getNewVersion().toFile();
        var appName = installerArgs.getAppName();

        deleteWithRetries(currVersion, log);

        IoUtils.copy(newVersion, currVersion);

        var isWindows = IoUtils.isWindowsOs();

        if (!isWindows) {
            IoUtils.setFilePermissions(currVersion, appName);
        }

        if (installerArgs.isRelaunch()) {
            var launcher = IoUtils.launcher(currVersion, appName, isWindows);

            if (!new File(launcher).canExecute()) {
                throw new RuntimeException("Cannot execute app launcher: " + launcher);
            }

            new ProcessBuilder(launcher)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
        }
    }

    // the current installation may be hard to delete while the app is still running, so
    // we need to try a few times before giving up as that allows for the current process to die.
    private static void deleteWithRetries(File currVersion,
                                          KeepupLogger log) throws IOException {
        int tries = 10;
        while (true) {
            tries--;
            try {
                IoUtils.deleteContents(currVersion);
                log.log("AppInstaller deleted old installation");
                return;
            } catch (IOException e) {
                log.log("AppInstaller failed to delete old installation. Tries left: " + tries);
                if (tries == 0) {
                    throw e;
                }
                try {
                    //noinspection BusyWait
                    Thread.sleep(500L);
                } catch (InterruptedException ignore) {
                }
            }
        }
    }

}
