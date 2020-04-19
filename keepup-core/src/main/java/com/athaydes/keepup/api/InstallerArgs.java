package com.athaydes.keepup.api;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
