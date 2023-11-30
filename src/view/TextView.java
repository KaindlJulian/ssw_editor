package view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
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
