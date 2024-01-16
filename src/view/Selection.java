package view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;

import java.util.List;

public class Selection {
    CharacterPosition start;
    CharacterPosition end;
    List<Line> linesBetween;
    private boolean enabled;

    public Selection(CharacterPosition start, CharacterPosition end, List<Line> linesBetween) {
        this.start = start;
        this.end = end;
        this.linesBetween = linesBetween;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getSelectedText() {
        StringBuilder sb = new StringBuilder();

        if (start.line == end.line) {
            sb.append(start.line.text, start.lineIndex, end.lineIndex + 1);
        } else {
            start.line.positions.stream().skip(start.lineIndex).forEach(p -> sb.append(p.sc));
            for (Line l : linesBetween) {
                sb.append(l.text);
            }
            end.line.positions.stream().takeWhile(p -> p.lineIndex < end.lineIndex).forEach(p -> sb.append(p.sc));
        }

        return sb.toString();
    }

    /**
     * assumes selection.start <= selection.end
     */
    public void draw(GraphicsContext g) {
        if (!enabled) return;

        g.setStroke(Paint.valueOf("000000"));
        g.setFill(Paint.valueOf("000000"));

        boolean onTheSameLine = start.line == end.line;

        // draw from start until end/line
        for (int i = start.lineIndex; i <= (onTheSameLine ? end.lineIndex : start.line.positions.size() - 1); i++) {
            CharacterPosition p = start.line.positions.get(i);
            double x = p.box.x;
            double y = p.box.y;
            double w = p.box.width;
            double h = p.box.height;
            g.strokeLine(x, y, x + w, y);
            g.strokeLine(x, y + h, x + w, y + h);
            if (i == start.lineIndex) {
                g.strokeLine(x, y, x, y + h);
            }
        }

        // draw for all positions in the lines between
        for (Line l : linesBetween) {
            for (CharacterPosition p : l.positions) {
                double x = p.box.x;
                double y = p.box.y;
                double w = p.box.width;
                double h = p.box.height;
                g.strokeLine(x, y, x + w, y);
                g.strokeLine(x, y + h, x + w, y + h);
            }
        }

        // draw from end until start/line
        for (int i = end.lineIndex; i >= (onTheSameLine ? start.lineIndex : 0); i--) {
            CharacterPosition p = end.line.positions.get(i);
            double x = p.box.x;
            double y = p.box.y;
            double w = p.box.width;
            double h = p.box.height;
            g.strokeLine(x, y, x + w, y);
            g.strokeLine(x, y + h, x + w, y + h);
            if (i == end.lineIndex) {
                g.strokeLine(x + w, y, x + w, y + h);
            }
        }
    }
}
