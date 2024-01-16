package view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;

enum CursorStyle {
    Line,
    Underline,
    Caret
}

public class Cursor {
    private final CursorStyle style;

    CharacterPosition position;
    private boolean enabled;

    public Cursor(CursorStyle style, CharacterPosition position) {
        this.style = style;
        this.position = position;
        this.enabled = true;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void moveUp() {
        if (position.line.prev != null) {
            if (position.line.prev.positions.size() < position.lineIndex) {
                position = position.line.prev.positions.getLast();
            } else {
                position = position.line.prev.positions.get(position.lineIndex);
            }
        }
    }

    public void moveDown() {
        if (position.line.next != null) {
            if (position.line.next.positions.size() < position.lineIndex) {
                position = position.line.prev.positions.getLast();
            } else {
                position = position.line.next.positions.get(position.lineIndex);
            }
        }
    }

    public void moveLeft() {
        if (position.lineIndex > 0) {
            position = position.line.positions.get(position.lineIndex - 1);
        } else if (position.line.prev != null) {
            position = position.line.prev.positions.getLast();
        }
    }

    public void moveRight() {
        if (position.lineIndex < position.line.positions.size() - 1) {
            position = position.line.positions.get(position.lineIndex + 1);
        } else if (position.line.next != null) {
            position = position.line.next.positions.getFirst();
        }
    }

    public void draw(GraphicsContext g) {
        if (!enabled) return;

        g.setStroke(Paint.valueOf("000000"));
        g.setFill(Paint.valueOf("000000"));

        double x = position.box.x;
        double y = position.box.y;
        double w = position.box.width;
        double h = position.box.height;
        double baseline = position.line.baseline;

        switch (style) {
            case Line -> g.strokeLine(x, y, x, y + h);
            case Underline -> g.strokeLine(x, y + h, x + w, y + h);
            case Caret -> {
                double caretWidth = 5;
                double caretHeight = 5;
                double[] xPoints = {x + w, x + w, x + w + caretWidth};
                double[] yPoints = {baseline, baseline + caretHeight, baseline + caretHeight};
                g.fillPolygon(xPoints, yPoints, 3);
            }
        }
    }
}
