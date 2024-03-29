package prototype;

import java.awt.*;
import java.awt.event.*;
import javax.swing.JScrollBar;
import java.util.Collections;

class Line {
    String text;    // text of this line
    int len;        // length of this line (including CRLF)
    int x, y, w, h; // top left corner, width, height
    int base;       // baseline
    Line prev, next;
}

class Position {
    Line line; // line containing this position
    int x, y;  // baseline point corresponding to this position
    int textPos;  // text position (relative to start of text)
    int org;   // origin (text position of first character in this line)
    int off;   // text offset from org
}

class Selection {
    Position beg, end;

    Selection(Position a, Position b) {
        beg = a;
        end = b;
    }
}

/*------------------------------------------------------------
 *  prototype.Viewer
 ------------------------------------------------------------*/

public class Viewer extends Canvas {
    static final int TOP = 5;    // top margin
    static final int BOTTOM = 5; // bottom margin
    static final int LEFT = 5;   // left margin
    static final int EOF = '\0';
    static final String CRLF = "\r\n";

    Text text;
    Line firstLine = null; // the lines in this viewer
    int firstTextPos = 0;     // first text position in this viewer
    int lastTextPos;          // last text position in this viewer
    Selection selection = null;     // current selection
    Position caret;                 // current caret
    Position lastPos;      // last mouse position: used during mouse dragging
    JScrollBar scrollBar;
    Graphics g;

    public Viewer(Text t, JScrollBar sb) {
        scrollBar = sb;
        scrollBar.setMaximum(t.length());
        scrollBar.setUnitIncrement(50);
        scrollBar.setBlockIncrement(500);
        scrollBar.addAdjustmentListener(this::doScroll);
        text = t;
        text.addUpdateEventListener(this::doUpdate);
        this.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                doKeyTyped(e);
            }

            public void keyPressed(KeyEvent e) {
                doKeyPressed(e);
            }
        });
        this.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                doMousePressed(e);
            }

            public void mouseReleased(MouseEvent e) {
                doMouseReleased(e);
            }
        });
        this.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                doMouseDragged(e);
            }
        });
        // disable TAB as a focus traversal key
        setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.emptySet());
    }

    /*------------------------------------------------------------
     *  scrolling
     *-----------------------------------------------------------*/

    public void doScroll(AdjustmentEvent e) {
        int pos = e.getValue();
        if (pos > 0) { // find start of line
            char ch;
            do {
                ch = text.charAt(--pos);
            } while (pos > 0 && ch != '\n');
            if (pos > 0) pos++;
        }
        if (pos != firstTextPos) { // scroll
            Position caret0 = caret;
            Selection sel0 = selection;
            removeSelection();
            removeCaret();
            firstTextPos = pos;
            firstLine = fill(TOP, getHeight() - BOTTOM, firstTextPos);
            repaint();
            if (caret0 != null) setCaret(caret0.textPos);
            if (sel0 != null) setSelection(sel0.beg.textPos, sel0.end.textPos);
        }
    }

    /*------------------------------------------------------------
     *  position handling
     *-----------------------------------------------------------*/

    private Position Pos(int textPos) {
        if (textPos < firstTextPos) textPos = firstTextPos;
        if (textPos > lastTextPos) textPos = lastTextPos;
        Position pos = new Position();
        setDefaultFont(g);
        Line line = firstLine, last = null;
        pos.org = firstTextPos;
        while (line != null && textPos >= pos.org + line.len) {
            pos.org += line.len;
            last = line;
            line = line.next;
        }
        if (line == null) {
            pos.x = last.x + last.w;
            pos.y = last.base;
            pos.line = last;
            pos.org -= last.len;
            pos.off = last.len;
        } else {
            pos.x = line.x;
            pos.y = line.base;
            pos.line = line;
            pos.off = textPos - pos.org;
            FontMetrics m = g.getFontMetrics();
            int i = pos.org;
            while (i < textPos) {
                char ch = text.charAt(i);
                i++;
                pos.x += charWidth(m, ch);
            }
        }
        pos.textPos = pos.org + pos.off;
        return pos;
    }

    private Position Pos(int x, int y) {
        Position pos = new Position();
        setDefaultFont(g);
        if (y >= getHeight() - BOTTOM) y = getHeight() - BOTTOM - 1;
        Line line = firstLine, last = null;
        pos.org = firstTextPos;
        while (line != null && y >= line.y + line.h) {
            pos.org += line.len;
            last = line;
            line = line.next;
        }
        if (line == null) {
            line = last;
            pos.org -= last.len;
        }
        pos.y = line.base;
        pos.line = line;
        if (x >= line.x + line.w) {
            pos.x = line.x + line.w;
            pos.off = line.len;
            if (pos.org + line.len < text.length()) pos.off -= 2;
        } else {
            pos.x = line.x;
            FontMetrics m = g.getFontMetrics();
            int i = pos.org;
            char ch = text.charAt(i);
            int w = charWidth(m, ch);
            while (x >= pos.x + w) {
                pos.x += w;
                i++;
                ch = text.charAt(i);
                w = charWidth(m, ch);
            }
            pos.off = i - pos.org;
        }
        pos.textPos = pos.org + pos.off;
        return pos;
    }

    /*------------------------------------------------------------
     *  caret handling
     *-----------------------------------------------------------*/

    private void invertCaret() {
        g = getGraphics();
        g.setXORMode(Color.WHITE);
        int x = caret.x;
        int y = caret.y;
        g.drawLine(x, y, x, y);
        y++;
        g.drawLine(x, y, x + 1, y);
        y++;
        g.drawLine(x, y, x + 2, y);
        y++;
        g.drawLine(x, y, x + 3, y);
        y++;
        g.drawLine(x, y, x + 4, y);
        y++;
        g.drawLine(x, y, x + 5, y);
        g.setPaintMode();
    }

    private void setCaret(Position pos) {
        removeCaret();
        removeSelection();
        caret = pos;
        invertCaret();
    }

    public void setCaret(int textPos) {
        if (textPos >= firstTextPos && textPos <= lastTextPos) {
            setCaret(Pos(textPos));
        } else caret = null;
    }

    public void setCaret(int x, int y) {
        setCaret(Pos(x, y));
    }

    public void removeCaret() {
        if (caret != null) invertCaret();
        caret = null;
    }

    /*------------------------------------------------------------
     *  selection handling
     *-----------------------------------------------------------*/

    private void invertSelection(Position beg, Position end) {
        g = getGraphics();
        setDefaultFont(g);
        g.setXORMode(Color.WHITE);
        Line line = beg.line;
        int x = beg.x;
        int y = line.y;
        int w;
        int h = line.h;
        while (line != end.line) {
            w = line.w + LEFT - x;
            g.fillRect(x, y, w, h);
            line = line.next;
            x = line.x;
            y = line.y;
        }
        w = end.x - x;
        g.fillRect(x, y, w, h);
        g.setPaintMode();
    }

    public void setSelection(int from, int to) {
        if (from < to) {
            removeCaret();
            Position beg = Pos(from);
            Position end = Pos(to);
            selection = new Selection(beg, end);
            invertSelection(beg, end);
        } else selection = null;
    }

    public void removeSelection() {
        if (selection != null) invertSelection(selection.beg, selection.end);
        selection = null;
    }

    /*------------------------------------------------------------
     *  keyboard handling
     *-----------------------------------------------------------*/

    private void doKeyTyped(KeyEvent e) {
        boolean selection = this.selection != null;
        if (selection) {
            text.delete(this.selection.beg.textPos, this.selection.end.textPos);
            // selection is removed; caret is set at sel.beg.textPos
        }
        if (caret != null) {
            char ch = e.getKeyChar();
            if (ch == KeyEvent.VK_BACK_SPACE) {
                if (caret.textPos > 0 && !selection) {
                    int d = caret.off == 0 ? 2 : 1;
                    text.delete(caret.textPos - d, caret.textPos);
                }
            } else if (ch == KeyEvent.VK_ESCAPE) {
                // TODO
                System.out.println("ESCAPE");
            } else if (ch == KeyEvent.VK_ENTER) {
                text.insert(caret.textPos, CRLF);
            } else {
                text.insert(caret.textPos, String.valueOf(ch));
            }
            scrollBar.setValues(firstTextPos, 0, 0, text.length());
        }
    }

    private void doKeyPressed(KeyEvent e) { // for cursor keys
        if (caret != null) {
            int key = e.getKeyCode();
            int pos = caret.textPos;
            char ch;
            if (key == KeyEvent.VK_RIGHT) {
                pos++;
                ch = text.charAt(pos);
                if (ch == '\n') pos++;
                setCaret(pos);
            } else if (key == KeyEvent.VK_LEFT) {
                pos--;
                ch = text.charAt(pos);
                if (ch == '\n') pos--;
                setCaret(pos);
            } else if (key == KeyEvent.VK_UP) {
                setCaret(caret.x, caret.y - caret.line.h);
            } else if (key == KeyEvent.VK_DOWN) {
                setCaret(caret.x, caret.y + caret.line.h);
            }
        }
    }

    /*------------------------------------------------------------
     *  mouse handling
     *-----------------------------------------------------------*/

    private void doMousePressed(MouseEvent e) {
        removeCaret();
        removeSelection();
        Position pos = Pos(e.getX(), e.getY());
        selection = new Selection(pos, pos);
        lastPos = pos;
    }

    private void doMouseDragged(MouseEvent e) {
        if (selection == null) return;
        Position pos = Pos(e.getX(), e.getY());
        if (pos.textPos < selection.beg.textPos) {
            if (lastPos.textPos == selection.end.textPos) {
                invertSelection(selection.beg, lastPos);
                selection.end = selection.beg;
            }
            invertSelection(pos, selection.beg);
            selection.beg = pos;
        } else if (pos.textPos > selection.end.textPos) {
            if (lastPos.textPos == selection.beg.textPos) {
                invertSelection(lastPos, selection.end);
                selection.beg = selection.end;
            }
            invertSelection(selection.end, pos);
            selection.end = pos;
        } else if (pos.textPos < lastPos.textPos) { // beg <= pos <= end; clear pos..end
            invertSelection(pos, selection.end);
            selection.end = pos;
        } else if (lastPos.textPos < pos.textPos) { // beg <= pos <= end; clear beg..pos
            invertSelection(selection.beg, pos);
            selection.beg = pos;
        }
        lastPos = pos;
    }

    private void doMouseReleased(MouseEvent e) {
        if (selection.beg.textPos == selection.end.textPos) setCaret(selection.beg);
        lastPos = null;
    }

    /*------------------------------------------------------------
     *  TAB handling
     *-----------------------------------------------------------*/

    private int charWidth(FontMetrics m, char ch) {
        if (ch == '\t') return 4 * m.charWidth(' ');
        else return m.charWidth(ch);
    }

    private int stringWidth(FontMetrics m, String s) {
        String s1 = s.replaceAll("\t", "    ");
        return m.stringWidth(s1);
    }

    private void drawString(Graphics g, String s, int x, int y) {
        String s1 = s.replaceAll("\t", "    ");
        g.drawString(s1, x, y);
    }

    /*------------------------------------------------------------
     *  line handling
     *-----------------------------------------------------------*/

    private Line fill(int top, int bottom, int pos) {
        g = getGraphics();
        setDefaultFont(g);
        FontMetrics m = g.getFontMetrics();
        Line first = null, line = null;
        int y = top;
        lastTextPos = pos;
        char ch = text.charAt(pos);
        while (y < bottom) {
            if (first == null) {
                first = line = new Line();
            } else {
                Line prev = line;
                line.next = new Line();
                line = line.next;
                line.prev = prev;
            }
            StringBuffer buf = new StringBuffer();
            while (ch != '\n' && ch != EOF) {
                buf.append(ch);
                pos++;
                ch = text.charAt(pos);
            }
            boolean eol = ch == '\n';
            if (eol) {
                buf.append(ch);
                pos++;
                ch = text.charAt(pos);
            }
            line.len = buf.length();
            line.text = buf.toString();
            line.x = LEFT;
            line.y = y;
            line.w = stringWidth(m, line.text);
            line.h = m.getHeight();
            line.base = y + m.getAscent();
            y += line.h;
            lastTextPos += line.len;
            if (!eol) break;
        }
        return first;
    }

    private void rebuildFrom(Position pos) {
        Line line = pos.line;
        Line prev = line.prev;
        line = fill(line.y, getHeight() - BOTTOM, pos.org);
        if (prev == null) firstLine = line;
        else {
            prev.next = line;
            line.prev = prev;
        }
        repaint(LEFT, line.y, getWidth(), getHeight());
    }

    /*------------------------------------------------------------
     *  text drawing
     *-----------------------------------------------------------*/

    public void doUpdate(UpdateEvent e) {
        StringBuffer b;
        g = getGraphics();
        setDefaultFont(g);
        FontMetrics m = g.getFontMetrics();
        Position pos = caret;

        if (e.from == e.to) { // insert
            if (e.from != caret.textPos) pos = Pos(e.from);
            int newCarPos = pos.textPos + e.text.length();
            if (e.text.contains(CRLF)) {
                rebuildFrom(pos);
                if (pos.y + pos.line.h > getHeight() - BOTTOM)
                    scrollBar.setValue(firstTextPos + firstLine.len);
            } else {
                b = new StringBuffer(pos.line.text);
                b.insert(pos.off, e.text);
                pos.line.text = b.toString();
                pos.line.w += stringWidth(m, e.text);
                pos.line.len += e.text.length();
                lastTextPos += e.text.length();
                repaint(pos.line.x, pos.line.y, getWidth(), pos.line.h + 1);
            }
            setCaret(newCarPos);

        } else if (e.text == null) { // delete
            if (caret == null || e.to != caret.textPos) pos = Pos(e.to);
            int d = e.to - e.from;
            if (pos.off - d < 0) { // delete across lines
                rebuildFrom(Pos(e.from));
            } else { // delete within a line
                b = new StringBuffer(pos.line.text);
                b.delete(pos.off - d, pos.off);
                pos.line.text = b.toString();
                pos.line.w = stringWidth(m, pos.line.text);
                pos.line.len -= d;
                lastTextPos -= d;
                repaint(pos.line.x, pos.line.y, getWidth(), pos.line.h + 1);
            }
            setCaret(e.from);
        }
    }

    private void setDefaultFont(Graphics g) {
        g.setFont(g.getFont().deriveFont(18f));
    }

    public void paint(Graphics g) {
        setDefaultFont(g);
        if (firstLine == null) {
            firstLine = fill(TOP, getHeight() - BOTTOM, 0);
            caret = Pos(0);
        }
        Line line = firstLine;
        while (line != null) {
            drawString(g, line.text, line.x, line.base);
            line = line.next;
        }
        if (caret != null) invertCaret();
        if (selection != null) invertSelection(selection.beg, selection.end);
    }
}
