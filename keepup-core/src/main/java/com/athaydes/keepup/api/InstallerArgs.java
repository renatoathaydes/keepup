package com.athaydes.keepup.api;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Arguments provided to the installer.
 * <p>
 * The installer is an application that runs inside the downloaded upgrade while the old application is
 * still installed on the local machine. Its job is to replace the old application with the new one.
 * <p>
 * Currently, only Keepup implements the installer, but in the future applications may provide custom
 * installer implementations, which would then be handled this set of arguments.
 */
public final class InstallerArgs {
    private final Path currentVersion;
    private final Path newVersion;
    private final String appName;
    private final boolean relaunch;

    public InstallerArgs(Path currentVersion, Path newVersion, String appName, boolean relaunch) {
        this.currentVersion = currentVersion;
        this.newVersion = newVersion;
        this.appName = appName;
        this.relaunch = relaunch;
    }

    public static InstallerArgs of(String[] mainArgs) {
        if (mainArgs.length < 4) {
            throw new IllegalArgumentException("Expected at least 4 arguments: " +
                    "currentVersion, newVersion, appName, relaunch|norelaunch.");
        }
        return new InstallerArgs(Paths.get(mainArgs[0]), Paths.get(mainArgs[1]),
                mainArgs[2], isRelaunch(mainArgs[3]));
    }

    private static boolean isRelaunch(String arg) {
        switch (arg) {
            case "relaunch":
                return true;
            case "norelaunch":
                return false;
            default:
                throw new IllegalArgumentException("Invalid argument (should be relaunch|norelaunch): " + arg);
        }
    }

    public Path getCurrentVersion() {
        return currentVersion;
    }

    public Path getNewVersion() {
        return newVersion;
    }

    public String getAppName() {
        return appName;
    }

    public boolean isRelaunch() {
        return relaunch;
    }

    public List<String> toArgs(boolean relaunch) {
        return List.of(
                currentVersion.toFile().getAbsolutePath(),
                newVersion.toFile().getAbsolutePath(),
                appName,
                relaunch ? "relaunch" : "norelaunch"
        );
    }
}
