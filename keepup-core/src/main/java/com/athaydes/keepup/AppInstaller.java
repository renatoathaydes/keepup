package com.athaydes.keepup;

import com.athaydes.keepup.api.InstallerArgs;

import java.io.File;
import java.util.Arrays;

final class AppInstaller {

    public static void main(String[] args) throws Exception {
        var installerArgs = InstallerArgs.of(args);

        File currVersion = installerArgs.getCurrentVersion().toFile();
        File newVersion = installerArgs.getNewVersion().toFile();
        String appName = installerArgs.getAppName();

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
