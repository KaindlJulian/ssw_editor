package view;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Line implements Iterable<Line> {
    String text;
    int textLength;
    int lineNumber;
    double baseline;
    BoundingBox box;
    List<CharacterPosition> positions;

    Line prev;
    Line next;

    public Stream<Line> toStream() {
        return toStream(false);
    }

    public Stream<Line> toStream(boolean reverse) {
        Spliterator<Line> spliterator = Spliterators.spliteratorUnknownSize(this.iterator(reverse), Spliterator.ORDERED);
        return StreamSupport.stream(spliterator, false);
    }

    public Line tail() {
        Line tail = this;
        for (Line t : this) {
            tail = t;
        }
        return tail;
    }

    @Override
    public Iterator<Line> iterator() {
        return iterator(false);
    }

    public Iterator<Line> iterator(boolean reverse) {
        Line self = this;
        return new Iterator<>() {
            Line current = self;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public Line next() {
                Line result = current;
                if (reverse) {
                    current = current.prev;
                } else {
                    current = current.next;
                }

                return result;
            }
        };
    }
}
