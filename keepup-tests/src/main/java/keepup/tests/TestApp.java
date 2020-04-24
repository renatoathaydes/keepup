package keepup.tests;

import com.athaydes.keepup.api.AppDistributor;
import com.athaydes.keepup.api.AppVersion;
import com.athaydes.keepup.api.Keepup;
import com.athaydes.keepup.api.KeepupConfig;
import com.athaydes.keepup.api.UpgradeInstaller;
import keepup.examples.trivial.Version;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

interface EnvVars {
    String ACCEPT_VERSION = "ACCEPT_VERSION";
    String NEW_VERSION = "NEW_VERSION";
    String LOG_ERRORS = "LOG_ERRORS";
    String LOG_NO_UPDATE = "LOG_NO_UPDATE";
    String LOG_DONE_NO_UPDATE = "LOG_DONE_NO_UPDATE";
    String ON_DONE = "ON_DONE";
    String EXIT_AFTER_MS = "EXIT_AFTER_MS";
}

enum OnDone {
    INSTALL_ON_EXIT, QUIT_AND_LAUNCH, ONLY_LAUNCH, NOTHING,
}

class TestApp {
    private static void log(Object message) {
        if (message instanceof Throwable) {
            ((Throwable) message).printStackTrace();
        }
        try {
            Files.writeString(Paths.get("build", "app.log"), message + "\n",
                    StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        log("TestApp version " + Version.CURRENT);

        var keepup = new Keepup(new TestConfig());

        var acceptVersion = System.getenv(EnvVars.ACCEPT_VERSION);

        if (acceptVersion != null) {
            keepup.onUpdate((v, file) -> CompletableFuture.completedFuture(
                    Boolean.parseBoolean(acceptVersion)));
        }

        var logError = System.getenv(EnvVars.LOG_ERRORS);

        if (Boolean.parseBoolean(logError)) {
            keepup.onError(TestApp::log);
        }

        var logNoUpdate = System.getenv(EnvVars.LOG_NO_UPDATE);

        if (Boolean.parseBoolean(logNoUpdate)) {
            keepup.onNoUpdateRequired(() -> log("No update"));
        }

        var onDone = OnDone.valueOf(Optional.ofNullable(System.getenv(EnvVars.ON_DONE)).orElse(OnDone.NOTHING.name()));

        var logDoneNoUpdate = Optional.ofNullable(System.getenv(EnvVars.LOG_DONE_NO_UPDATE))
                .map(Boolean::parseBoolean)
                .orElse(false);

        Runnable doneNoUpdate = logDoneNoUpdate ? () -> log("Done without update") : Keepup.NO_OP;

        switch (onDone) {
            case INSTALL_ON_EXIT:
                keepup.onDone(doneNoUpdate, UpgradeInstaller::installUpgradeOnExit);
                break;
            case QUIT_AND_LAUNCH:
                keepup.onDone(doneNoUpdate, UpgradeInstaller::quitAndLaunchUpgradedApp);
                break;
            case ONLY_LAUNCH:
                keepup.onDone(doneNoUpdate, UpgradeInstaller::launchUpgradedAppWithoutExiting);
                break;
            case NOTHING:
                break;
        }

        keepup.createUpdater().checkForUpdate();

        // always sleep a little bit in order to not kill Keepup before it does anything
        var exitAfterMs = Optional.ofNullable(System.getenv(EnvVars.EXIT_AFTER_MS))
                .map(Long::parseLong)
                .orElse(500L);

        Thread.sleep(exitAfterMs);

        log("Exiting");
        keepup.close();
    }
}

class TestConfig implements KeepupConfig {
    @Override
    public String appName() {
        return "tests";
    }

    @Override
    public File appHome() {
        return new File("build");
    }

    @Override
    public AppDistributor<?> distributor() {
        return new TestDistributor();
    }
}

class TestDistributor implements AppDistributor<AppVersion> {

    @Override
    public Optional<AppVersion> findLatestVersion() {
        var v = System.getenv(EnvVars.NEW_VERSION);
        if (v == null) return Optional.empty();
        return Optional.of(AppVersion.ofString(v));
    }

    @Override
    public File download(AppVersion version) {
        return new File("build", "test-app.zip");
    }
}