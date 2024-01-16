package view;

/**
 * A character in a line.
 */
public class CharacterPosition {
    final StyledCharacter sc;

    /**
     * Absolute position of the character in the whole text.
     */
    final int textPosition;

    /**
     * Absolute position of the character in the line.
     */
    final int lineIndex;

    /**
     * The bounding box of the character.
     */
    final BoundingBox box;

    /**
     * Reference to the line of this character
     */
    final Line line;

    public CharacterPosition(StyledCharacter sc, int textPosition, int lineIndex, BoundingBox box, Line line) {
        this.sc = sc;
        this.textPosition = textPosition;
        this.lineIndex = lineIndex;
        this.box = box;
        this.line = line;
    }
}
