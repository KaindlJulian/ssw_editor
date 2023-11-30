package text;

import javax.swing.text.Style;
import java.awt.*;
import java.io.File;

public class StyledPiece extends Piece {
    Font font;
    Style style;

    public StyledPiece(File file, int offset, int length, Font font, Style style) {
        super(file, offset, length);
        this.font = font;
        this.style = style;
    }
}
