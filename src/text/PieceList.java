package text;

import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import view.StyledCharacter;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.ByteBuffer;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class PieceList implements Iterable<Piece> {
    int totalLength;
    final Piece firstPiece;
    final File scratch;
    final FileWriter scratchWriter;
    private final List<UpdateEventListener> listeners;

    public PieceList(Piece firstPiece) throws IOException {
        this.firstPiece = firstPiece;
        this.totalLength = firstPiece.length;
        this.scratch = new File(".scratch");
        this.scratch.delete();
        this.scratch.createNewFile();
        this.scratchWriter = new FileWriter(scratch, StandardCharsets.UTF_8);
        this.listeners = new ArrayList<>();
    }

    /**
     * Split at given position.
     *
     * @param position Position to place the split after. The character at this position is part of the left split.
     * @return The left side of the split
     */
    private Piece split(int position) {
        // find piece containing the position
        Piece p = firstPiece;
        int length = p.length;
        while (position > length) {
            p = p.next;
            length += p.length;
        }
        // split piece p at given position
        if (position != length) {
            int split2 = length - position;
            int split1 = p.length - split2;
            p.length = split1;
            Piece q = new Piece(p.file, p.offset + split1, split2, p.font, p.color);
            q.next = p.next;
            p.next = q;
        }
        return p;
    }

    /**
     * Inserts a character at a position in the text.
     *
     * @param position  The position after which the character should be inserted.
     * @param character The character (UTF-8).
     */
    public void insert(int position, int character) {
        Piece p = split(position);

        // p is not the last piece on the scratch file
        if (p.file != scratch || p.length + p.offset < scratch.length()) {
            Piece q = new Piece(scratch, (int) scratch.length(), 0);
            q.next = p.next;
            p.next = q;
            p = q;
        }

        try {
            scratchWriter.write(character);
            scratchWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        p.length++;
        totalLength++;
        fireUpdateEvent(new UpdateEvent.Insert(position, String.valueOf((char) character)));
    }

    /**
     * Inserts a string at a position in the text.
     *
     * @param position The position after which the character should be inserted.
     * @param text     The string.
     */
    public void insert(int position, String text) {
        Piece p = split(position);

        // p is not the last piece on the scratch file
        if (p.file != scratch || p.length + p.offset < scratch.length()) {
            Piece q = new Piece(scratch, (int) scratch.length(), 0);
            q.next = p.next;
            p.next = q;
            p = q;
        }

        for (char character : text.toCharArray()) {
            try {
                scratchWriter.write(character);
                scratchWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        p.length += text.length();
        totalLength += text.length();
        fireUpdateEvent(new UpdateEvent.Insert(position, text));
    }

    /**
     * Deletes all text within a range.
     *
     * @param from Delete text after this position.
     * @param to   Delete text before and at this position.
     */
    public void delete(int from, int to) {
        Piece a = split(from);
        Piece b = split(to);
        a.next = b.next;
        totalLength = totalLength - (to - from);
        fireUpdateEvent(new UpdateEvent.Delete(from, to));
    }

    /**
     * @param from  Set style after this position
     * @param to    Set style before and at this position
     * @param font  The font name and size
     * @param color The color
     */
    public void setStyle(int from, int to, Font font, Paint color) {
        Piece a = split(from);
        Piece b = split(to);

        Piece p = new Piece(null, 0, 0);
        p.next = a.next;

        while (p != null && p != b.next) {
            if (font != null) {
                p.font = font;
            }
            if (color != null) {
                p.color = color;
            }
            p = p.next;
        }
    }

    public void addUpdateEventListener(UpdateEventListener l) {
        this.listeners.add(l);
    }

    public void removeUpdateEventListener(UpdateEventListener l) {
        this.listeners.remove(l);
    }

    private void fireUpdateEvent(UpdateEvent e) {
        listeners.forEach(l -> l.update(e));
    }

    /**
     * @return the character at given position
     */
    public char readCharAt(int position) {
        // find piece containing the position
        Piece p = firstPiece;
        int length = p.length;
        while (position > length - 1) {
            if (p.next == null) {
                return '\0';
            }
            p = p.next;
            length += p.length;
        }

        int offset = p.offset + p.length - length + position;

        try (RandomAccessFile f = new RandomAccessFile(p.file, "r")) {
            f.seek(offset);
            byte[] bytes = new byte[4]; // Maximum bytes needed to represent a UTF-8 character
            f.read(bytes);

            Charset utf8Charset = Charset.forName("UTF-8");
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            char character = utf8Charset.decode(buffer).charAt(0);

            return character;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public StyledCharacter readStyledCharAt(int position) {
        // find piece containing the position
        Piece p = firstPiece;
        int length = p.length;
        while (position > length - 1) {
            if (p.next == null) {
                return null;
            }
            p = p.next;
            length += p.length;
        }

        int offset = p.offset + p.length - length + position;

        try (RandomAccessFile f = new RandomAccessFile(p.file, "r")) {
            f.seek(offset);
            byte[] bytes = new byte[4]; // Maximum bytes needed to represent a UTF-8 character
            f.read(bytes);

            Charset utf8Charset = StandardCharsets.UTF_8;
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            char character = utf8Charset.decode(buffer).charAt(0);

            return new StyledCharacter(character, p.font, p.color);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Save the piece list to the original file.
     */
    public void save() throws IOException {
        byte[] styleMetadata = styleMetadataLine().getBytes();
        byte[] fileBuffer = new byte[styleMetadata.length + totalLength];

        System.arraycopy(styleMetadata, 0, fileBuffer, 0, styleMetadata.length);

        int currentIndex = styleMetadata.length;
        for (Piece p : this) {
            try (RandomAccessFile f = new RandomAccessFile(p.file, "r")) {
                byte[] buffer = new byte[p.length];
                f.seek(p.offset);
                f.readFully(buffer);
                System.arraycopy(buffer, 0, fileBuffer, currentIndex, buffer.length);
                currentIndex += buffer.length;
            }
        }

        Files.write(firstPiece.file.toPath(), fileBuffer, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private String styleMetadataLine() {
        StringBuilder sb = new StringBuilder();
        int textPosition = 0;
        for (Piece p : this) {
            sb.append(textPosition);
            sb.append(",");
            sb.append(p.length);
            sb.append(",");
            sb.append(p.font.getName());
            sb.append(",");
            sb.append(p.font.getSize());
            sb.append(",");
            sb.append(p.color);
            sb.append("|");
            textPosition += p.length;
        }
        return sb + "\n";
    }

    public int getTotalLength() {
        return this.totalLength;
    }

    @Override
    public Iterator<Piece> iterator() {
        return new Iterator<>() {
            Piece current = firstPiece;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public Piece next() {
                Piece result = current;
                current = current.next;
                return result;
            }
        };
    }
}