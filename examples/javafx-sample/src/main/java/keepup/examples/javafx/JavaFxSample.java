package keepup.examples.javafx;

import com.athaydes.keepup.api.AppDistributor;
import com.athaydes.keepup.api.AppVersion;
import com.athaydes.keepup.api.Keepup;
import com.athaydes.keepup.api.KeepupConfig;
import com.athaydes.keepup.api.UpgradeInstaller;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static keepup.examples.javafx.Components.button;
import static keepup.examples.javafx.Components.label;
import static keepup.examples.javafx.Components.link;
import static keepup.examples.javafx.Components.text;

public class JavaFxSample extends Application {

    private final Keepup keepup = new Keepup(new SampleConfig());

    @Override
    public void start(Stage primaryStage) {
        var mainBox = new VBox(10);
        mainBox.setAlignment(Pos.CENTER);
        mainBox.getChildren().addAll(
                text(Version.BANNER, "banner"),
                label("Version " + Version.CURRENT),
                label("This sample demonstrates how a JavaFX app can self-update easily with Keepup."),
                link("https://github.com/renatoathaydes/keepup", getHostServices())
        );
        var scene = new Scene(new BorderPane(mainBox), 600.0, 200.0);
        scene.getStylesheets().add(JavaFxSample.class.getResource("styles.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Keepup Examples");
        primaryStage.centerOnScreen();
        primaryStage.show();

        initializeKeepup(primaryStage);
    }

    @Override
    public void stop() {
        keepup.close();
    }

    private void initializeKeepup(Stage primaryStage) {
        keepup.onUpdate((newVersion, zipFile) -> {
            var completer = new CompletableFuture<Boolean>();
            Platform.runLater(() -> {
                var dialog = new Dialog(primaryStage);
                dialog.addAll(text("There is a new update!"),
                        label("Do you want to update to version " + newVersion + "?"),
                        new HBox(20, button("Update App!", () -> {
                            dialog.hide();
                            completer.complete(true);
                        }), button("Not now", () -> {
                            dialog.hide();
                            completer.complete(false);
                        })));
                dialog.show();
            });
            return completer;
        }).onError((error) -> {
            Platform.runLater(() -> {
                var dialog = new Dialog(primaryStage);
                dialog.addAll(text("ERROR!", "error"),
                        label("Cannot update due to " + error),
                        button("OK", dialog::hide));
                dialog.show();
            });
        }).onDone(Keepup.NO_OP, UpgradeInstaller::quitAndLaunchUpgradedApp);

        var exec = (ScheduledExecutorService) keepup.getConfig().executor();

        // we check for updates almost immediately on start up, then, every 5 seconds!
        // This is just a demo, normally you wouldn't check nearly as often, perhaps once a day or a week.
        exec.scheduleAtFixedRate(keepup.createUpdater()::checkForUpdate, 3L, 5, TimeUnit.SECONDS);
    }

    public static void main(String[] args) {
        System.out.println("Launching JavaFxSample");
        launch(args);
    }
}

class SampleConfig implements KeepupConfig {

    @Override
    public String appName() {
        return "fx_sample";
    }

    @Override
    public AppDistributor<?> distributor() {
        // silly implementation that updates every time to the same version
        return new SampleDistributor();
    }

    @Override
    public File appHome() {
        return new File("build");
    }

    @Override
    public ExecutorService executor() {
        return Executors.newScheduledThreadPool(2);
    }
}

class SampleDistributor implements AppDistributor<AppVersion> {

    private final Set<String> versions = new HashSet<>();

    @Override
    public CompletionStage<Optional<AppVersion>> findLatestVersion() {
        // notice that because we return a Future, we could ask the user in the UI Thread
        // whether or not to download the update before returning the new version.
        var future = new CompletableFuture<Optional<AppVersion>>();

        // any file in the build directory with a name like "update-.*.zip" is treated as an update
        var files = new File("build").listFiles();
        if (files != null) for (var file : files) {
            var fname = file.getName();
            if (fname.matches("update-.*\\.zip")) {
                String version = fname.substring("update-".length(), fname.length() - 4);
                if (versions.add(version)) {
                    future.complete(Optional.of(AppVersion.ofString(version)));
                }
            }
        }

        future.complete(Optional.empty());

        return future;
    }

    @Override
    public File download(AppVersion version) {
        return new File("build", "update-" + version.name() + ".zip");
    }
}
