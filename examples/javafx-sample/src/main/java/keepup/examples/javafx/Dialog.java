package keepup.examples.javafx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class Dialog {
    private final Stage dialogStage = new Stage();
    private final VBox box = new VBox(10);

    public Dialog(Stage primaryStage) {
        dialogStage.initOwner(primaryStage);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        dialogStage.setScene(new Scene(box));
        dialogStage.getScene().getStylesheets().add(JavaFxSample.class.getResource("styles.css").toExternalForm());
        dialogStage.getScene().setFill(Color.TRANSPARENT);
    }

    public void addAll(Node... children) {
        box.getChildren().addAll(children);
    }

    public void show() {
        dialogStage.show();
    }

    public void hide() {
        dialogStage.hide();
    }
}
