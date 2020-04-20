package keepup.examples.javafx;

import javafx.application.HostServices;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.text.Text;

final class Components {

    static Text text(String text, String... classes) {
        var node = new Text(text);
        node.getStyleClass().addAll(classes);
        return node;
    }

    static Label label(String text, String... classes) {
        var node = new Label(text);
        node.getStyleClass().addAll(classes);
        return node;
    }

    static Hyperlink link(String href, HostServices hostServices) {
        var node = new Hyperlink(href);
        node.setOnAction(e -> hostServices.showDocument(href));
        return node;
    }

    static Button button(String text, Runnable onClick) {
        var button = new Button(text);
        button.setOnAction((e) -> onClick.run());
        return button;
    }

}
