import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import text.Piece;
import text.PieceList;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PieceListTest {
    @BeforeEach
    public void beforeEach() throws IOException {
        new File(".scratch").delete();
        new File(".scratch").createNewFile();
    }

    @Test
    public void test() throws IOException {
        File originalFile = new File("test-files/one-line.txt");
        Piece first = new Piece(originalFile, 0, (int)originalFile.length());
        PieceList pl = new PieceList(first);

        pl.insert(8, 'A');
        pl.insert(9, 'A');

        printPieceList(pl);

        pl.delete(9, 10);
        pl.delete(8, 9);

        printPieceList(pl);

        pl.insert(8, 'A');

        printPieceList(pl);
    }

    private void printPieceList(PieceList pl) {
        int position = 0;
        char character = pl.readCharAt(position);
        position++;
        StringBuilder s = new StringBuilder();
        while (character != '\0' && character != '\n') {
            s.append(character);
            character = pl.readCharAt(position);
            position++;
        }
        System.out.println(s);
    }
}
