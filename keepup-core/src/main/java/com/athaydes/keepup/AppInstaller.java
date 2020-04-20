package com.athaydes.keepup;

import com.athaydes.keepup.api.InstallerArgs;

import java.io.File;
import java.util.Arrays;

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
            run(installerArgs);
            log.log("AppInstaller successful");
        } catch (Exception e) {
            log.log("ERROR: AppInstaller - " + e);
        }
    }

    private static void run(InstallerArgs installerArgs) throws Exception {
        var currVersion = installerArgs.getCurrentVersion().toFile();
        var newVersion = installerArgs.getNewVersion().toFile();
        var appName = installerArgs.getAppName();

        IoUtils.deleteContents(currVersion);

        String[] children = currVersion.list();
        if (children != null && children.length != 0) {
            throw new RuntimeException("Did not delete everything, error! " + Arrays.toString(children));
        }

        IoUtils.copy(newVersion, currVersion);
        IoUtils.setFilePermissions(currVersion, appName);

        if (installerArgs.isRelaunch()) {
            if (!new File(currVersion, "bin/" + appName).canExecute()) {
                throw new RuntimeException("Cannot execute app launcher: " + currVersion + "/bin/" + appName);
            }

            new ProcessBuilder(getLauncher(currVersion, appName))
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
        }
    }

    private static String getLauncher(File currVersion, String appName) {
        String os = System.getProperty("os.name", "");
        String launcherFile;
        if (os.contains("Windows")) {
            launcherFile = "bin\\" + appName + ".bat";
        } else {
            launcherFile = "bin/" + appName;
        }
        return new File(currVersion, launcherFile).getAbsolutePath();
    }

}
