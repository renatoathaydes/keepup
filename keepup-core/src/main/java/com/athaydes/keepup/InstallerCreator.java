package com.athaydes.keepup;

import com.athaydes.keepup.api.InstallerArgs;
import com.athaydes.keepup.api.Keepup;
import com.athaydes.keepup.api.KeepupConfig;
import com.athaydes.keepup.api.UpgradeInstaller;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static com.athaydes.keepup.IoUtils.currentApp;
import static com.athaydes.keepup.IoUtils.unpackedApp;

final class InstallerCreator {

    static UpgradeInstaller create(KeepupConfig config) {
        var currVersion = currentApp().toPath();
        var newVersion = unpackedApp(config.appHome()).toPath();
        return new Installer(config, new InstallerArgs(currVersion, newVersion,
                config.keepupLog(), config.appName(), false));
    }

    private static class Installer implements UpgradeInstaller {
        private final AtomicBoolean done = new AtomicBoolean(false);
        private final KeepupLogger log;
        private final InstallerArgs args;

        public Installer(KeepupConfig config,
                         InstallerArgs installerArgs) {
            this.log = new KeepupLogger(config.keepupLog());
            this.args = installerArgs;
        }

        @Override
        public void launchUpgradedAppWithoutExiting() {
            checkNotDone();
            log.log("Invoking installer");
            invokeInstaller(true);
        }

        @Override
        public void quitAndLaunchUpgradedApp() {
            launchUpgradedAppWithoutExiting();
            log.log("Exiting process");
            System.exit(0);
        }

        @Override
        public void installUpgradeOnExit() {
            checkNotDone();
            log.log("Will run installer on JVM shutdown");
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
                var proc = new ProcessBuilder(command)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();

                // we try to wait until the process is done as in some platforms, the running software
                // can be replaced in place, so the process would probably terminate in time... but in
                // others (Windows) we must kill the current process before installation completes.
                var done = proc.waitFor(2, TimeUnit.SECONDS);

                if (done) {
                    if (proc.exitValue() != 0) {
                        log.log("ERROR: Installer exited with " + proc.exitValue());
                    }
                } else {
                    log.log("Installer did not end in time");
                }
            } catch (IOException | InterruptedException e) {
                log.log("ERROR: " + e);
            }
        }

        private void checkNotDone() {
            if (done.getAndSet(true)) {
                throw new IllegalStateException("Action already performed");
            }
        }
    }
}
