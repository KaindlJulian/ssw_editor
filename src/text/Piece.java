package text;

import java.io.File;

public class Piece {
    File file;
    int offset;
    int length;
    Piece next;

    public Piece(File file, int offset, int length) {
        this.file = file;
        this.offset = offset;
        this.length = length;
    }
}
