package text;

import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

import java.io.File;

public class Piece {
    private static final Font defaultFont = javafx.scene.text.Font.font("Arial", 32);
    private static final Paint defaultColor = Paint.valueOf("000000");

    File file;
    int offset;
    int length;
    Piece next;

    Font font;
    Paint color;

    public Piece(File file, int offset, int length) {
        this(file, offset, length, defaultFont, defaultColor);
    }

    public Piece(File file, int offset, int length, Font font, Paint color) {
        this.file = file;
        this.offset = offset;
        this.length = length;
        this.font = font;
        this.color = color;
    }
}
