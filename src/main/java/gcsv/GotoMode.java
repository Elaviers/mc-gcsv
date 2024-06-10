package gcsv;

public enum GotoMode {
    NONE(0),
    ALWAYS(1),
    NEVER(2),
    ASK(3);

    public final int id;

    GotoMode(int id) {
        this.id = id;
    }

    public static GotoMode fromId(int id)
    {
        return switch (id) {
            case 1 -> ALWAYS;
            case 2 -> NEVER;
            case 3 -> ASK;
            default -> NONE;
        };
    }
}
