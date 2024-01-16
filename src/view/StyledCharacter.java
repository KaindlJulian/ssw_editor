package view;

import com.sun.javafx.tk.FontMetrics;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

public class StyledCharacter {
    final char character;
    final Font font;
    final Paint color;
    final FontMetrics fm;

    public StyledCharacter(char character, Font font, Paint color) {
        this.character = character;
        this.font = font;
        this.color = color;
        this.fm = Toolkit.getToolkit().getFontLoader().getFontMetrics(font);
    }

    public double getCharacterWidth() {
        return Util.charWidth(fm, character);
    }

    public double getLineHeight() {
        return fm.getLineHeight();
    }
}
