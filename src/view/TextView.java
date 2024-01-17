package view;

import clipboard.Clipboard;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import text.Piece;
import text.PieceList;
import text.UpdateEvent;
import text.UpdateEvent.*;
import text.UpdateEventListener;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TextView extends Canvas implements UpdateEventListener {
    private static final int MARGIN = 5;
    private static final int SCROLL_LINES = 3;

    private final PieceList text;
    private final GraphicsContext g;

    // reference to the first line that is displayed in the view
    private Line firstLine;

    // the two positions specify the section of the text that is visible in the editor
    private int firstTextPosition;
    private int lastTextPosition;

    private final Cursor cursor;
    private Selection selection;

    public TextView(File file, double width, double height) throws IOException {
        super(width, height);

        widthProperty().addListener(e -> refillFromPosAndDraw(firstTextPosition));
        heightProperty().addListener(e -> refillFromPosAndDraw(firstTextPosition));

        this.g = getGraphicsContext2D();
        this.text = new PieceList(new Piece(file, 0, (int) file.length()));
        this.text.addUpdateEventListener(this);
        this.firstTextPosition = 0;
        this.lastTextPosition = 0;
        this.firstLine = fill(MARGIN, getHeight() - MARGIN, 0);

        this.cursor = new Cursor(CursorStyle.Line, firstLine.positions.getFirst());
        this.selection = new Selection(firstLine.positions.getFirst(), firstLine.positions.getFirst(), List.of(firstLine));
        this.selection.setEnabled(false);
    }

    /**
     * Create doubly linked list of lines that fill the available vertical space.
     *
     * @param from              y value of starting point
     * @param to                y value of end point
     * @param startTextPosition text position to start with
     */
    private Line fill(double from, double to, int startTextPosition) {
        double y = from;
        int position = startTextPosition;
        lastTextPosition = startTextPosition;

        Line line = new Line();
        Line first = line;
        int lineNumber = 0;

        // fill the vertical space
        while (true) {
            StringBuilder lineText = new StringBuilder();
            List<CharacterPosition> positions = new ArrayList<>();
            double xOffset = MARGIN;
            double maxCharacterHeight = 0.0;
            StyledCharacter biggestCharacter = null;

            StyledCharacter sc = text.readStyledCharAt(position);
            //sc = new StyledCharacter(sc.character, new Font("Arial", 48), sc.color);
            position++;
            while (sc != null && sc.character != '\n') {
                double characterWidth = sc.getCharacterWidth();
                BoundingBox characterBox = new BoundingBox(xOffset, y, characterWidth, 0.0); // height is set at end of line
                positions.add(new CharacterPosition(sc, position - 1, lineText.length(), characterBox, line));
                xOffset += characterWidth;
                if (sc.getLineHeight() >= maxCharacterHeight) {
                    maxCharacterHeight = sc.getLineHeight();
                    biggestCharacter = sc;
                }
                lineText.append(sc.character);
                sc = text.readStyledCharAt(position);
                position++;
            }

            boolean isEOF = sc == null;

            // handle newline
            if (!isEOF) {
                lineText.append(sc.character);
                BoundingBox characterBox = new BoundingBox(xOffset, y, sc.getCharacterWidth(), 0.0); // height is set at end of line
                positions.add(new CharacterPosition(sc, position - 1, lineText.length() - 1, characterBox, line));
            }

            line.textLength = lineText.length();
            line.text = lineText.toString();
            line.lineNumber = lineNumber;
            line.positions = positions;
            assert biggestCharacter != null;
            final double lineHeight = biggestCharacter.getLineHeight();
            line.box = new BoundingBox(MARGIN, y, xOffset - MARGIN, lineHeight);
            line.baseline = y + biggestCharacter.fm.getAscent();
            // fixup the height for all characters, to match the largest character in this line
            line.positions.forEach(p -> p.box.height = lineHeight);

            y += line.box.height;

            if (!line.positions.isEmpty()) {
                lastTextPosition = line.positions.getLast().textPosition;
            } else {
                lastTextPosition++;
            }

            if (isEOF) {
                break;
            }

            if (y > to) {
                break;
            }

            lineNumber++;
            Line prev = line;
            line.next = new Line();
            line = line.next;
            line.prev = prev;
        }

        return first;
    }

    private void draw() {
        // cover background
        g.setFill(Paint.valueOf("f2f2f2"));
        g.fillRect(0, 0, getWidth(), getHeight());

        for (Line l : firstLine) {
            for (CharacterPosition p : l.positions) {
                g.setFont(p.sc.font);
                g.setFill(p.sc.color);
                g.fillText(String.valueOf(p.sc.character), p.box.x, l.baseline);
            }
        }

        cursor.draw(g);
        selection.draw(g);
        //drawDebug();
    }

    private void drawDebug() {
        g.setStroke(Paint.valueOf("ff0000"));
        // view bounds
        g.strokeRect(0, 0, getWidth(), getHeight());
        // line bounds and baseline
        for (Line l : firstLine) {
            BoundingBox box = l.box;
            g.strokeRect(box.x, box.y, box.width, box.height);
            g.strokeLine(box.x, l.baseline, box.x + box.width, l.baseline);
        }
        // character bounds
        g.setStroke(Paint.valueOf("008800"));
        for (Line l : firstLine) {
            for (CharacterPosition p : l.positions) {
                BoundingBox box = p.box;
                g.strokeRect(box.x, box.y, box.width, box.height);
            }
        }
    }

    private void refillFromPosAndDraw(int pos) {
        firstTextPosition = pos;
        firstLine = fill(MARGIN, getHeight() - MARGIN, pos);
        int cursorPos = cursor.position.textPosition;
        cursor.setEnabled(firstTextPosition <= cursorPos && cursorPos <= lastTextPosition);
        draw();
    }

    private Line refillFromLineAndDraw(Line line) {
        Line prev = line.prev;
        line = fill(line.box.y, getHeight() - MARGIN, line.positions.getFirst().textPosition);
        if (prev == null) {
            firstLine = line;
        } else {
            prev.next = line;
            line.prev = prev;
        }
        int cursorPos = cursor.position.textPosition;
        cursor.setEnabled(firstTextPosition <= cursorPos && cursorPos <= lastTextPosition);
        draw();
        return line;
    }

    public void scroll(int pos) {
        while (text.readCharAt(pos) != '\n' && pos > 0) {
            pos--;
        }
        if (pos > 0) pos++;
        refillFromPosAndDraw(pos);
    }

    public int getNextScrollAmount() {
        return firstLine.toStream().limit(SCROLL_LINES).mapToInt(l -> l.textLength).sum();
    }

    public int getTextLength() {
        return text.getTotalLength();
    }

    public void handleKey(KeyEvent e) {
        switch (e.getCode()) {
            case BACK_SPACE -> handleDeleteKey(e);
            case UP, DOWN, LEFT, RIGHT -> handleCursorKey(e);
            default -> {
                if (e.getText().isEmpty()) break;

                String t = e.getText();
                if (e.getCode().isLetterKey() && e.isShiftDown()) {
                    t = t.toUpperCase();
                }

                text.insert(cursor.position.textPosition, t.charAt(0));
            }
        }
        selection.setEnabled(false);
        cursor.setEnabled(true);
        draw();
    }

    private void handleDeleteKey(KeyEvent e) {
        if (e.getCode() == KeyCode.BACK_SPACE) {
            if (cursor.isEnabled()) {
                text.delete(cursor.position.textPosition - 1, cursor.position.textPosition);
            } else if (selection.isEnabled()) {
                text.delete(selection.start.textPosition, selection.end.textPosition + 1);
            }
        }
    }

    private void handleCursorKey(KeyEvent e) {
        switch (e.getCode()) {
            case UP -> {
                if (cursor.position.line.prev == null && firstTextPosition > 1 && lastTextPosition > 0) {
                    scroll(firstTextPosition - 2);
                    Line l = firstLine;
                    cursor.position = cursor.position.lineIndex < l.textLength
                            ? l.positions.get(cursor.position.lineIndex)
                            : l.positions.getLast();
                } else {
                    cursor.moveUp();
                }
            }
            case DOWN -> {
                if (cursor.position.line.next == null && lastTextPosition < text.getTotalLength() - 1) {
                    scroll(firstLine.next.positions.getFirst().textPosition);
                    Line l = firstLine.tail();
                    cursor.position = cursor.position.lineIndex < l.textLength
                            ? l.positions.get(cursor.position.lineIndex)
                            : l.positions.getLast();
                } else {
                    cursor.moveDown();
                }
            }
            case LEFT -> {
                if (selection.isEnabled()) {
                    cursor.position = selection.start;
                } else if (cursor.position.textPosition == firstTextPosition && firstTextPosition > 0) {
                    scroll(firstTextPosition - 1);
                    cursor.position = firstLine.positions.getLast();
                } else {
                    cursor.moveLeft();
                }
            }
            case RIGHT -> {
                if (selection.isEnabled()) {
                    cursor.position = selection.end.line.positions.get(selection.end.lineIndex + 1);
                } else if (cursor.position.textPosition == lastTextPosition && lastTextPosition < text.getTotalLength() - 1) {
                    scroll(firstLine.next.positions.getFirst().textPosition);
                    cursor.position = firstLine.tail().positions.getFirst();
                } else {
                    cursor.moveRight();
                }
            }
        }
    }

    public void handleDoubleClick(MouseEvent e) {
        CharacterPosition clickedCharacter = characterPositionFromCoordinates(e.getX(), e.getY() - 15);
        if (clickedCharacter == null) return;
        Line line = clickedCharacter.line;
        int startPos = clickedCharacter.lineIndex;
        while (startPos > 0 && line.positions.get(startPos - 1).sc.character != ' ' && line.positions.get(startPos).sc.character != '\n') {
            startPos--;
        }
        int endPos = clickedCharacter.lineIndex;
        while (endPos < line.positions.size() && line.positions.get(endPos).sc.character != ' ' && line.positions.get(endPos).sc.character != '\n') {
            endPos++;
        }
        selection = new Selection(line.positions.get(startPos), line.positions.get(endPos - 1), List.of());
        selection.setEnabled(true);
        cursor.setEnabled(false);
        cursor.position = selection.start;
        draw();
    }

    public void handleMousePressed(MouseEvent e) {
        selection.setEnabled(false);
        selection.start = characterPositionFromCoordinates(e.getX(), e.getY() - 15);
        selection.end = characterPositionFromCoordinates(e.getX(), e.getY() - 15);
    }

    public void handleMouseDragged(MouseEvent e) {
        CharacterPosition p = characterPositionFromCoordinates(e.getX(), e.getY() - 15);
        if (p == null) return;
        if (selection.start.textPosition < p.textPosition) {
            selection.end = characterPositionFromCoordinates(e.getX(), e.getY() - 15);
        } else {
            selection.start = characterPositionFromCoordinates(e.getX(), e.getY() - 15);
        }
        selection.linesBetween = linesBetweenPositions(selection.start, selection.end);
        selection.setEnabled(true);
        cursor.setEnabled(false);
        cursor.position = selection.start;
        draw();
    }


    @Override
    public void update(UpdateEvent e) {
        switch (e) {
            case Insert(int pos, String t) -> {
                Line l = refillFromLineAndDraw(lineFromTextPosition(Math.max(pos - 1, 0)));
                cursor.position = l.positions.get(cursor.position.lineIndex + t.length());
            }
            case Delete(int from, int to) -> {
                refillFromLineAndDraw(lineFromTextPosition(from));
                cursor.position = characterPositionFromTextPosition(from);
            }
        }
        draw();
    }

    private CharacterPosition characterPositionFromCoordinates(double x, double y) {
        for (Line l : firstLine) {
            for (CharacterPosition p : l.positions) {
                if (p.box.containsPoint(x, y)) {
                    return p;
                }
            }
        }
        return null;
    }

    private CharacterPosition characterPositionFromTextPosition(int pos) {
        for (Line l : firstLine) {
            for (CharacterPosition p : l.positions) {
                if (p.textPosition == pos) {
                    return p;
                }
            }
        }
        return firstLine.positions.getFirst();
    }

    private Line lineFromTextPosition(int pos) {
        for (Line l : firstLine) {
            for (CharacterPosition p : l.positions) {
                if (p.textPosition == pos) {
                    return l;
                }
            }
        }
        return firstLine;
    }

    private List<Line> linesBetweenPositions(CharacterPosition start, CharacterPosition end) {
        List<Line> lines = new ArrayList<>();
        for (Line l : start.line) {
            if (l.lineNumber < end.line.lineNumber && l != start.line) {
                lines.add(l);
            }
        }
        return lines;
    }

    public void handleClipboardCopy() {
        if (selection.isEnabled()) {
            Clipboard.getInstance().setContent(selection.getSelectedText());
        }
    }

    public void handleClipboardCut() {
        if (selection.isEnabled()) {
            Clipboard.getInstance().setContent(selection.getSelectedText());
            selection.setEnabled(false);
            cursor.setEnabled(true);
            text.delete(selection.start.textPosition, selection.end.textPosition + 1);
        }
    }

    public void handleClipboardPaste() {
        if (cursor.isEnabled()) {
            text.insert(cursor.position.textPosition, Clipboard.getInstance().getContent());
        }
    }

    public void handleSave() throws IOException {
        text.save();
    }

    public void handleSetFont(Font font) {
        if (selection.isEnabled()) {
            text.setStyle(selection.start.textPosition, selection.end.textPosition + 1, font, null);
            refillFromPosAndDraw(firstTextPosition);
        }
    }

    public void handleSetColor(Paint color) {
        if (selection.isEnabled()) {
            text.setStyle(selection.start.textPosition, selection.end.textPosition + 1, null, color);
            refillFromPosAndDraw(firstTextPosition);
        }
    }

    public boolean handleSearch(String searchWord) {
        for (Line l : firstLine) {
            int startIndex = l.text.indexOf(searchWord);
            if (startIndex != -1) {
                CharacterPosition start = l.positions.get(startIndex);
                CharacterPosition end = l.positions.get(startIndex + searchWord.length() - 1);
                selection = new Selection(start, end, List.of());
                selection.setEnabled(true);
                cursor.setEnabled(false);
                draw();
                return true;
            }
        }
        return false;
    }
}
