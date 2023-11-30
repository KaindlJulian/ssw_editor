package text;

public sealed interface UpdateEvent permits UpdateEvent.Insert, UpdateEvent.Delete {
    record Insert(int position, String text) implements UpdateEvent {
    }

    record Delete(int from, int to) implements UpdateEvent {
    }
}
