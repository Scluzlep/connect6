package core.board;

public enum PieceColor {
    BLACK, WHITE, EMPTY;

    public PieceColor opposite() {
        if (this == BLACK) return WHITE;
        if (this == WHITE) return BLACK;
        return EMPTY;
    }
}