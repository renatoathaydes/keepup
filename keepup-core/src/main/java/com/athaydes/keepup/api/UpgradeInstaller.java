package com.athaydes.keepup.api;

public interface UpgradeInstaller {
    void launchUpgradedAppWithoutExiting();

    void quitAndLaunchUpgradedApp();

    void installUpgradeOnExit();
}
