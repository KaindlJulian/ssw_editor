import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser;
import view.TextView;

import java.io.File;
import java.io.IOException;

public class Controller {

    @FXML
    TabPane tabPane;

    @FXML
    private void initialize() {
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (o != null) {
                o.getStyleClass().remove("current-tab");
            }
            if (n != null) {
                n.getStyleClass().add("current-tab");
            }
        });
    }

    @FXML
    protected void onMenuOpen() throws IOException {
        // choose file to open
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Text File");
        fileChooser.setInitialDirectory(new File("."));
        File file = fileChooser.showOpenDialog(tabPane.getScene().getWindow());
        // create new editor view
        TextView view = new TextView(file, 1000, 1000); // todo dimensions
        Tab tab = new Tab(file.getName(), new ScrollPane(view));
        tab.setTooltip(new Tooltip(file.getAbsolutePath()));
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
        tabPane.requestLayout();
    }

    @FXML
    protected void onMenuSave() {
        System.out.println("onMenuSave");
    }

    @FXML
    protected void onMenuClose() {
        System.out.println("onMenuClose");
    }

    @FXML
    protected void onMenuCopy() {
        System.out.println("onMenuCopy");
    }

    @FXML
    protected void onMenuCut() {
        System.out.println("onMenuCut");
    }

    @FXML
    protected void onMenuPaste() {
        System.out.println("onMenuPaste");
    }
}
