package view;

import com.sun.javafx.tk.FontMetrics;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import text.Piece;
import text.PieceList;
import text.UpdateEvent;
import text.UpdateEvent.*;
import text.UpdateEventListener;

import java.io.File;
import java.io.IOException;

public class TextView extends Canvas implements UpdateEventListener {
    final PieceList text;
    final GraphicsContext g;



    public TextView(File file, double width, double height) throws IOException {
        super(width, height);
        this.text = new PieceList(new Piece(file, 0, (int) file.length())); // todo fix for unicode
        this.text.addUpdateEventListener(this);
        this.g = getGraphicsContext2D();
        draw();
    }

    private void draw() {
        FontMetrics fm = com.sun.javafx.tk.Toolkit.getToolkit().getFontLoader().getFontMetrics(Font.font("Arial", 20));
        g.fillText("is.text = new PieceList(new", 15, 15);
    }

    @Override
    public void update(UpdateEvent e) {
        switch (e) {
            case Insert(int pos, String t) -> {

            }
            case Delete(int from, int to) -> {

            }
        }
    }
}
