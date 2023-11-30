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
        File originalFile = new File("test-files/example.txt");
        Piece first = new Piece(originalFile, 0, (int)originalFile.length());
        PieceList pl = new PieceList(first);

        pl.insert(1, "<".getBytes(StandardCharsets.UTF_8)[0]);
        pl.insert(2, ">".getBytes(StandardCharsets.UTF_8)[0]);

        pl.save();
    }
}
