import com.sun.javafx.tk.Toolkit;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import view.TextView;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Controller {


    @FXML
    TabPane tabPane;

    @FXML
    TextField searchInput;

    @FXML
    TextField colorInput;

    @FXML
    TextField fontSizeInput;

    @FXML
    ChoiceBox<String> fontChoiceBox;

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
        tabPane.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            TextView activeView = getActiveView();
            if (activeView != null && !e.isShortcutDown()) {
                activeView.handleKey(e);
                e.consume();
            }
        });
        tabPane.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            TextView activeView = getActiveView();
            if (activeView != null && e.getY() >= 30 && e.getX() < activeView.getWidth() - 15) {
                activeView.handleMousePressed(e);
                e.consume();
            }
        });
        tabPane.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            TextView activeView = getActiveView();
            if (activeView != null && e.getY() >= 30 && e.getX() < activeView.getWidth() - 15) {
                activeView.handleMouseDragged(e);
                e.consume();
            }
        });
        tabPane.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            TextView activeView = getActiveView();
            if (activeView != null && e.getY() >= 30 && e.getX() < activeView.getWidth() - 15) {
                activeView.handleMouseDragged(e);
                e.consume();
            }
        });
        tabPane.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getY() < 30) return;
            TextView activeView = getActiveView();
            if (activeView != null && e.getClickCount() == 2) {
                activeView.handleDoubleClick(e);
                e.consume();
            }
        });

        ObservableList<String> fontNames = FXCollections.observableArrayList(Toolkit.getToolkit().getFontLoader().getFontNames());
        fontChoiceBox.setItems(fontNames);
    }

    @FXML
    protected void onMenuOpen() throws IOException {
        // choose file to open
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Text File");
        fileChooser.setInitialDirectory(new File("./test-files"));
        File file = fileChooser.showOpenDialog(tabPane.getScene().getWindow());

        if (file == null) {
            return;
        }

        // create new editor view
        TextView view = new TextView(file, tabPane.getWidth(), tabPane.getHeight());
        view.widthProperty().bind(tabPane.widthProperty().subtract(14));
        view.heightProperty().bind(tabPane.heightProperty().subtract(36));

        // setup scrollbar for this view
        ScrollBar scrollBar = new ScrollBar();
        scrollBar.setOrientation(Orientation.VERTICAL);
        scrollBar.maxHeightProperty().bind(view.heightProperty());

        scrollBar.valueProperty().addListener((obs, o, n) -> {
            view.scroll(n.intValue());
        });

        view.setOnScroll(e -> {
            scrollBar.setMax(view.getTextLength());
            if (e.getDeltaY() < 0) {
                int scrollAmount = view.getNextScrollAmount();
                if (scrollAmount > 0) {
                    scrollBar.setUnitIncrement(scrollAmount);
                }
            }
            scrollBar.fireEvent(e);
        });

        // add tab
        StackPane stackPane = new StackPane(view, scrollBar);
        StackPane.setAlignment(view, Pos.TOP_LEFT);
        StackPane.setAlignment(scrollBar, Pos.TOP_RIGHT);
        Tab tab = new Tab(file.getName(), stackPane);
        tab.setTooltip(new Tooltip(file.getAbsolutePath()));
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    @FXML
    protected void onMenuSave() throws IOException {
        if (!tabPane.getSelectionModel().isEmpty()) {
            StackPane sp = (StackPane) tabPane.getSelectionModel().getSelectedItem().getContent();
            TextView v = (TextView) sp.getChildrenUnmodifiable().get(0);
            v.handleSave();
        }
    }

    @FXML
    protected void onMenuSaveAll() {
        System.out.println("onMenuSaveAll");
    }

    @FXML
    protected void onMenuCopy() {
        if (!tabPane.getSelectionModel().isEmpty()) {
            StackPane sp = (StackPane) tabPane.getSelectionModel().getSelectedItem().getContent();
            TextView v = (TextView) sp.getChildrenUnmodifiable().get(0);
            v.handleClipboardCopy();
        }
    }

    @FXML
    protected void onMenuCut() {
        if (!tabPane.getSelectionModel().isEmpty()) {
            StackPane sp = (StackPane) tabPane.getSelectionModel().getSelectedItem().getContent();
            TextView v = (TextView) sp.getChildrenUnmodifiable().get(0);
            v.handleClipboardCut();
        }
    }

    @FXML
    protected void onMenuPaste() {
        if (!tabPane.getSelectionModel().isEmpty()) {
            StackPane sp = (StackPane) tabPane.getSelectionModel().getSelectedItem().getContent();
            TextView v = (TextView) sp.getChildrenUnmodifiable().get(0);
            v.handleClipboardPaste();
        }
    }

    @FXML
    protected void onSearchNext() {
        if (!tabPane.getSelectionModel().isEmpty()) {
            StackPane sp = (StackPane) tabPane.getSelectionModel().getSelectedItem().getContent();
            TextView v = (TextView) sp.getChildrenUnmodifiable().get(0);
            if (v.handleSearch(searchInput.getText())) {
                searchInput.setStyle("-fx-text-fill: green;");
            } else {
                searchInput.setStyle("-fx-text-fill: red;");
            }
        }
    }


    @FXML
    protected void onSetFont() {
        System.out.println(fontChoiceBox.getValue());
    }

    @FXML
    protected void onSetColor() {
        colorInput.clear();
    }

    @FXML
    protected void onSetFontSize() {
        fontSizeInput.clear();
    }

    private TextView getActiveView() {
        if (!tabPane.getSelectionModel().isEmpty()) {
            StackPane sp = (StackPane) tabPane.getSelectionModel().getSelectedItem().getContent();
            return (TextView) sp.getChildrenUnmodifiable().get(0);
        }
        return null;
    }
}
