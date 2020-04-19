package com.athaydes.keepup;

import com.athaydes.keepup.api.InstallerArgs;
import com.athaydes.keepup.api.Keepup;
import com.athaydes.keepup.api.KeepupConfig;
import com.athaydes.keepup.api.UpgradeInstaller;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static com.athaydes.keepup.IoUtils.currentApp;
import static com.athaydes.keepup.IoUtils.unpackedApp;

final class InstallerCreator {

    static UpgradeInstaller create(KeepupConfig config) {
        var currVersion = currentApp().toPath();
        var newVersion = unpackedApp(config.appHome()).toPath();
        return new Installer(config, new InstallerArgs(currVersion, newVersion, config.appName(), false));
    }

    private static class Installer implements UpgradeInstaller {
        private final AtomicBoolean done = new AtomicBoolean(false);
        private final KeepupConfig config;
        private final InstallerArgs args;

        public Installer(KeepupConfig config,
                         InstallerArgs installerArgs) {
            this.config = config;
            this.args = installerArgs;
        }

        @Override
        public void launchUpgradedAppWithoutExiting() {
            checkNotDone();
            config.logger().accept("Invoking installer");
            invokeInstaller(true);
            config.logger().accept("Installer successful");
        }

        @Override
        public void quitAndLaunchUpgradedApp() {
            launchUpgradedAppWithoutExiting();
            config.logger().accept("Exiting process");
            System.exit(0);
        }

        @Override
        public void installUpgradeOnExit() {
            checkNotDone();
            config.logger().accept("Will run installer on JVM shutdown");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> invokeInstaller(false)));
        }

        private void invokeInstaller(boolean relaunch) {
            String[] command = Stream.concat(
                    List.of(new File(args.getNewVersion().toFile(), "bin/java").getAbsolutePath(),
                            "-m",
                            Keepup.class.getModule().getName() + "/" + AppInstaller.class.getName()).stream(),
                    args.toArgs(relaunch).stream()
            ).toArray(String[]::new);

            try {
                var exitCode = new ProcessBuilder(command)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start()
                        .waitFor();
                if (exitCode != 0) {
                    config.logger().accept("ERROR: Installer exited with " + exitCode);
                }
            } catch (IOException | InterruptedException e) {
                config.logger().accept("ERROR: " + e);
            }
        }

        private void checkNotDone() {
            if (done.getAndSet(true)) {
                throw new IllegalStateException("Action already performed");
            }
        }
    }
}
