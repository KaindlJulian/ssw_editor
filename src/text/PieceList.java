package text;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
            Piece q = new Piece(p.file, p.offset + split1, split2);
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
    public void insert(int position, int character) throws IOException {
        Piece p = split(position);

        // p is not the last piece on the scratch file
        if (p.file != scratch || p.length + p.offset != scratch.length()) {
            Piece q = new Piece(scratch, 0, (int) scratch.length());
            q.next = p.next;
            p.next = q;
            p = q;
        }

        scratchWriter.write(character);
        scratchWriter.flush();
        p.length++;
        totalLength++;
        fireUpdateEvent(new UpdateEvent.Insert(position, ""));
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
        fireUpdateEvent(new UpdateEvent.Delete(from, to));
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
     * Save the piece list to the original file.
     */
    public void save() throws IOException {
        byte[] fileBuffer = new byte[totalLength];
        int currentIndex = 0;

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