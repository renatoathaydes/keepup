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
            System.exit(54);
            return;
        }

        System.out.println("AppInstaller running");

        try {
            run(installerArgs);
            System.out.println("AppInstaller successful");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(55);
        }
    }

    private static void run(InstallerArgs installerArgs) throws Exception {
        var currVersion = installerArgs.getCurrentVersion().toFile();
        var newVersion = installerArgs.getNewVersion().toFile();
        var appName = installerArgs.getAppName();

        deleteWithRetries(currVersion);

        IoUtils.copy(newVersion, currVersion);

        var isWindows = IoUtils.isWindowsOs();

        if (!isWindows) {
            IoUtils.setFilePermissions(currVersion, appName);
        }

        if (installerArgs.isRelaunch()) {
            var launcher = IoUtils.launcher(currVersion, appName, isWindows);

            if (!new File(launcher).canExecute()) {
                System.exit(57);
                return;
            }

            new ProcessBuilder(launcher)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
        }
    }

    // the current installation may be hard to delete while the app is still running, so
    // we need to try a few times before giving up as that allows for the current process to die.
    private static void deleteWithRetries(File currVersion) {
        int tries = 10;
        while (true) {
            tries--;
            try {
                IoUtils.deleteContents(currVersion);
                System.out.println("AppInstaller deleted old installation");
                return;
            } catch (IOException e) {
                System.out.println("AppInstaller failed to delete old installation. Tries left: " + tries);
                if (tries <= 0) {
                    e.printStackTrace();
                    System.exit(56);
                    return;
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
