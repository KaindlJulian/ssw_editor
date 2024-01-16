package view;

import com.sun.javafx.tk.FontMetrics;

public class Util {
    public static double stringWidth(FontMetrics fm, String s) {
        String s1 = s.replaceAll("\t", "    ");
        return s1.chars().mapToDouble(c -> (double) fm.getCharWidth((char) c)).sum();
    }

    public static double charWidth(FontMetrics fm, char c) {
        return fm.getCharWidth(c);
    }
}
