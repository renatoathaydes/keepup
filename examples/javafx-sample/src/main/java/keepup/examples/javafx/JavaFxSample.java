package keepup.examples.javafx;

import com.athaydes.keepup.api.AppDistributor;
import com.athaydes.keepup.api.Keepup;
import com.athaydes.keepup.api.KeepupConfig;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.File;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JavaFxSample extends Application {

    private final Keepup keepup = new Keepup(new SampleConfig());

    @Override
    public void start(Stage primaryStage) {
        var mainBox = new VBox(10);
        mainBox.setAlignment(Pos.CENTER);
        mainBox.getChildren().addAll(
                new Text(Version.BANNER),
                new Label("Version " + Version.CURRENT),
                new Label("This sample demonstrates how a JavaFX app can self-update easily with Keepup."),
                new Hyperlink("https://github.com/renatoathaydes/keepup")
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
                dialog.addAll(new Text("There is a new update!"),
                        new Label("Do you want to update to version " + newVersion + "?"),
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
                dialog.addAll(new Text("ERROR!"),
                        new Label("Cannot update due to " + error),
                        button("OK", dialog::hide));
                dialog.show();
            });
        }).onDone((installer) -> {
            // the installer is only provided on a successful update...
            // on errors or not-accepted updates, it would be null
            if (installer != null) {
                installer.quitAndLaunchUpgradedApp();
            }
        });

        var exec = (ScheduledExecutorService) keepup.getConfig().executor();

        // we check for updates almost immediately on start up, then, every 5 seconds!
        // This is just a demo, normally you wouldn't check nearly as often, perhaps once a day or a week.
        exec.scheduleAtFixedRate(keepup.createUpdater()::checkForUpdate, 3L, 5, TimeUnit.SECONDS);
    }

    private static Button button(String text, Runnable onClick) {
        var button = new Button(text);
        button.setOnAction((e) -> onClick.run());
        return button;
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
    public AppDistributor distributor() {
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

class SampleDistributor implements AppDistributor {

    private final Set<String> versions = new HashSet<>();

    @Override
    public Optional<String> findLatestVersion() {
        // any file in the build directory with a name like "update-.*.zip" is treated as an update
        var files = new File("build").listFiles();
        if (files != null) for (var file : files) {
            var fname = file.getName();
            if (fname.matches("update-.*\\.zip")) {
                String version = fname.substring("update-".length(), fname.length() - 4);
                if (versions.add(version)) {
                    return Optional.of(version);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public File download(String version) {
        return new File("build", "update-" + version + ".zip");
    }
}